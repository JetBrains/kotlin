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
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.CaptureStatus
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.contains
import java.util.*

class ConstraintInjector(val constraintIncorporator: ConstraintIncorporator, val typeApproximator: TypeApproximator) {
    private val ALLOWED_DEPTH_DELTA_FOR_INCORPORATION = 3

    interface Context {
        val allTypeVariables: Map<TypeConstructor, NewTypeVariable>

        var maxTypeDepthFromInitialConstraints: Int
        val notFixedTypeVariables: MutableMap<TypeConstructor, MutableVariableWithConstraints>

        fun addInitialConstraint(initialConstraint: InitialConstraint)
        fun addError(error: KotlinCallDiagnostic)
    }

    fun addInitialSubtypeConstraint(c: Context, lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition) {
        c.addInitialConstraint(InitialConstraint(lowerType, upperType, ConstraintKind.UPPER, position))
        updateAllowedTypeDepth(c, lowerType)
        updateAllowedTypeDepth(c, upperType)
        addSubTypeConstraintAndIncorporateIt(c, lowerType, upperType, position)
    }

    fun addInitialEqualityConstraint(c: Context, a: UnwrappedType, b: UnwrappedType, position: ConstraintPosition) {
        c.addInitialConstraint(InitialConstraint(a, b, ConstraintKind.EQUALITY, position))
        updateAllowedTypeDepth(c, a)
        updateAllowedTypeDepth(c, b)
        addSubTypeConstraintAndIncorporateIt(c, a, b, position)
        addSubTypeConstraintAndIncorporateIt(c, b, a, position)
    }


    private fun addSubTypeConstraintAndIncorporateIt(c: Context, lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition) {
        val incorporatePosition = IncorporationConstraintPosition(position)
        val possibleNewConstraints = Stack<Pair<NewTypeVariable, Constraint>>()
        val typeCheckerContext = TypeCheckerContext(c, position, lowerType, upperType, possibleNewConstraints)
        typeCheckerContext.runIsSubtypeOf(lowerType, upperType)

        while (possibleNewConstraints.isNotEmpty()) {
            val (typeVariable, constraint) = possibleNewConstraints.pop()
            val constraints = c.notFixedTypeVariables[typeVariable.freshTypeConstructor] ?: typeCheckerContext.fixedTypeVariable(typeVariable)

            // it is important, that we add constraint here(not inside TypeCheckerContext), because inside incorporation we read constraints
            constraints.addConstraint(constraint)?.let {
                constraintIncorporator.incorporate(typeCheckerContext, typeVariable, it, incorporatePosition)
            }
        }
    }

    private fun updateAllowedTypeDepth(c: Context, initialType: UnwrappedType) {
        c.maxTypeDepthFromInitialConstraints = Math.max(c.maxTypeDepthFromInitialConstraints, initialType.typeDepth())
    }

    private fun UnwrappedType.typeDepth() =
            when (this) {
                is SimpleType -> typeDepth()
                is FlexibleType -> Math.max(lowerBound.typeDepth(), upperBound.typeDepth())
            }

    private fun SimpleType.typeDepth(): Int {
        val maxInArguments = arguments.asSequence().map {
            if (it.isStarProjection) 1 else it.type.unwrap().typeDepth()
        }.max() ?: 0

        return maxInArguments + 1
    }

    private fun Context.isAllowedType(type: UnwrappedType) = type.typeDepth() <= maxTypeDepthFromInitialConstraints + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION

    private inner class TypeCheckerContext(
            val c: Context,
            val position: ConstraintPosition,
            val baseLowerType: UnwrappedType,
            val baseUpperType: UnwrappedType,
            val possibleNewConstraints: MutableList<Pair<NewTypeVariable, Constraint>> = ArrayList()
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
                addConstraint(typeVariable, superType, ConstraintKind.UPPER)

        override fun addLowerConstraint(typeVariable: TypeConstructor, subType: UnwrappedType) =
                addConstraint(typeVariable, subType, ConstraintKind.LOWER)

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
                if (kind == ConstraintKind.UPPER) {
                    val subType = typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation)
                    if (subType != null && !KotlinBuiltIns.isNothingOrNullableNothing(subType)) {
                        targetType = subType
                    }
                }

                if (kind == ConstraintKind.LOWER) {
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

            if (!c.isAllowedType(targetType)) return

            val newConstraint = Constraint(kind, targetType, position)
            possibleNewConstraints.add(typeVariable to newConstraint)
        }

        // from ConstraintIncorporator.Context
        override fun addNewIncorporatedConstraint(lowerType: UnwrappedType, upperType: UnwrappedType, position: IncorporationConstraintPosition) {
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