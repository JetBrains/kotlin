/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind.LOWER
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind.UPPER
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.CaptureStatus
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isNullableAny
import java.util.*

class ConstraintInjector(val constraintIncorporator: ConstraintIncorporator, val typeApproximator: TypeApproximator) {
    private val ALLOWED_DEPTH_DELTA_FOR_INCORPORATION = 1

    interface Context {
        val allTypeVariables: Map<TypeConstructor, NewTypeVariable>

        var maxTypeDepthFromInitialConstraints: Int
        val notFixedTypeVariables: MutableMap<TypeConstructor, MutableVariableWithConstraints>

        fun addInitialConstraint(initialConstraint: InitialConstraint)
        fun addError(error: KotlinCallDiagnostic)
    }

    fun addInitialSubtypeConstraint(c: Context, lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition) {
        val initialConstraint = InitialConstraint(lowerType, upperType, UPPER, position)
        val incorporationPosition = IncorporationConstraintPosition(position, initialConstraint)
        c.addInitialConstraint(initialConstraint)
        updateAllowedTypeDepth(c, lowerType)
        updateAllowedTypeDepth(c, upperType)
        addSubTypeConstraintAndIncorporateIt(c, lowerType, upperType, incorporationPosition)
    }

    fun addInitialEqualityConstraint(c: Context, a: UnwrappedType, b: UnwrappedType, position: ConstraintPosition) {
        val initialConstraint = InitialConstraint(a, b, ConstraintKind.EQUALITY, position)
        val incorporationPosition = IncorporationConstraintPosition(position, initialConstraint)
        c.addInitialConstraint(initialConstraint)
        updateAllowedTypeDepth(c, a)
        updateAllowedTypeDepth(c, b)
        addSubTypeConstraintAndIncorporateIt(c, a, b, incorporationPosition)
        addSubTypeConstraintAndIncorporateIt(c, b, a, incorporationPosition)
    }


    private fun addSubTypeConstraintAndIncorporateIt(c: Context, lowerType: UnwrappedType, upperType: UnwrappedType, incorporatePosition: IncorporationConstraintPosition) {
        val possibleNewConstraints = Stack<Pair<NewTypeVariable, Constraint>>()
        val typeCheckerContext = TypeCheckerContext(c, incorporatePosition, lowerType, upperType, possibleNewConstraints)
        typeCheckerContext.runIsSubtypeOf(lowerType, upperType)

        while (possibleNewConstraints.isNotEmpty()) {
            val (typeVariable, constraint) = possibleNewConstraints.pop()
            if (c.shouldWeSkipConstraint(typeVariable, constraint)) continue

            val constraints = c.notFixedTypeVariables[typeVariable.freshTypeConstructor] ?: typeCheckerContext.fixedTypeVariable(typeVariable)

            // it is important, that we add constraint here(not inside TypeCheckerContext), because inside incorporation we read constraints
            constraints.addConstraint(constraint)?.let {
                constraintIncorporator.incorporate(typeCheckerContext, typeVariable, it)
            }
        }
    }

    private fun updateAllowedTypeDepth(c: Context, initialType: UnwrappedType) {
        c.maxTypeDepthFromInitialConstraints = Math.max(c.maxTypeDepthFromInitialConstraints, initialType.typeDepth())
    }

    private fun Context.shouldWeSkipConstraint(typeVariable: NewTypeVariable, constraint: Constraint): Boolean {
        assert(constraint.kind != ConstraintKind.EQUALITY)

        val constraintType = constraint.type
        if (!isAllowedType(constraintType)) return true

        if (constraintType.constructor == typeVariable.freshTypeConstructor) {
            if (constraintType.lowerIfFlexible().isMarkedNullable && constraint.kind == LOWER) return false // T? <: T

            return true // T <: T(?!)
        }

        if (constraintType is SimpleType) {
            if (constraint.kind == UPPER && constraintType.isNullableAny()) return true // T <: Any?
            if (constraint.kind == LOWER && constraintType.isNothing()) return true // T >: Nothing
        }

        return false
    }

    private fun Context.isAllowedType(type: UnwrappedType) = type.typeDepth() <= maxTypeDepthFromInitialConstraints + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION

    private inner class TypeCheckerContext(
            val c: Context,
            val position: IncorporationConstraintPosition,
            val baseLowerType: UnwrappedType,
            val baseUpperType: UnwrappedType,
            val possibleNewConstraints: MutableList<Pair<NewTypeVariable, Constraint>>
    ) : TypeCheckerContextForConstraintSystem(), ConstraintIncorporator.Context {

        fun runIsSubtypeOf(lowerType: UnwrappedType, upperType: UnwrappedType) {
            with(NewKotlinTypeChecker) {
                if (!this@TypeCheckerContext.isSubtypeOf(lowerType, upperType)) {
                    // todo improve error reporting -- add information about base types
                    c.addError(NewConstraintError(lowerType, upperType, position))
                }
            }
        }

        // from TypeCheckerContextForConstraintSystem
        override fun isMyTypeVariable(type: SimpleType): Boolean = c.allTypeVariables.containsKey(type.constructor)
        override fun addUpperConstraint(typeVariable: TypeConstructor, superType: UnwrappedType) =
                addConstraint(typeVariable, superType, UPPER)

        override fun addLowerConstraint(typeVariable: TypeConstructor, subType: UnwrappedType) =
                addConstraint(typeVariable, subType, LOWER)

        private fun isCapturedTypeFromSubtyping(type: UnwrappedType) =
                when ((type as? NewCapturedType)?.captureStatus) {
                    null, CaptureStatus.FROM_EXPRESSION -> false
                    CaptureStatus.FOR_SUBTYPING -> true
                    CaptureStatus.FOR_INCORPORATION ->
                        error("Captured type for incorporation shouldn't escape from incorporation: $type\n" + renderBaseConstraint())
                }

        private fun addConstraint(typeVariableConstructor: TypeConstructor, type: UnwrappedType, kind: ConstraintKind) {
            val typeVariable = c.allTypeVariables[typeVariableConstructor]
                               ?: error("Should by type variableConstructor: $typeVariableConstructor. ${c.allTypeVariables.values}")

            var targetType = type
            if (type.contains(this::isCapturedTypeFromSubtyping)) {
                // TypeVariable <: type -> if TypeVariable <: subType => TypeVariable <: type
                if (kind == UPPER) {
                    val subType = typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation)
                    if (subType != null && !KotlinBuiltIns.isNothingOrNullableNothing(subType)) {
                        targetType = subType
                    }
                }

                if (kind == LOWER) {
                    val superType = typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation)
                    if (superType != null && !KotlinBuiltIns.isAnyOrNullableAny(superType)) { // todo rethink error reporting for Any cases
                        targetType = superType
                    }
                }

                if (targetType === type) {
                    c.addError(CapturedTypeFromSubtyping(typeVariable, type, position))
                    return
                }
            }

            possibleNewConstraints.add(typeVariable to Constraint(kind, targetType, position))
        }

        // from ConstraintIncorporator.Context
        override fun addNewIncorporatedConstraint(lowerType: UnwrappedType, upperType: UnwrappedType) {
            if (c.isAllowedType(lowerType) && c.isAllowedType(upperType)) {
                runIsSubtypeOf(lowerType, upperType)
            }
        }

        override val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>
            get() = c.notFixedTypeVariables.values

        override fun getTypeVariable(typeConstructor: TypeConstructor): NewTypeVariable? {
            val typeVariable = c.allTypeVariables[typeConstructor]
            if (typeVariable != null && !c.notFixedTypeVariables.containsKey(typeConstructor)) {
                fixedTypeVariable(typeVariable)
            }
            return typeVariable
        }

        override fun getConstraintsForVariable(typeVariable: NewTypeVariable) =
                c.notFixedTypeVariables[typeVariable.freshTypeConstructor]?.constraints
                ?: fixedTypeVariable(typeVariable)

        fun fixedTypeVariable(variable: NewTypeVariable): Nothing {
            error("Type variable $variable should not be fixed!\n" +
                  renderBaseConstraint())
        }

        private fun renderBaseConstraint() = "Base constraint: $baseLowerType <: $baseUpperType from position: $position"
    }
}