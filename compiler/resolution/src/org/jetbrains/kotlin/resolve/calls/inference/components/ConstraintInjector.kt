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


import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind.LOWER
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind.UPPER
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import java.util.*
import kotlin.math.max

class ConstraintInjector(
    val constraintIncorporator: ConstraintIncorporator,
    val typeApproximator: AbstractTypeApproximator,
    val kotlinTypeRefiner: KotlinTypeRefiner
) {
    private val ALLOWED_DEPTH_DELTA_FOR_INCORPORATION = 1

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        var maxTypeDepthFromInitialConstraints: Int
        val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>
        val fixedTypeVariables: MutableMap<TypeConstructorMarker, KotlinTypeMarker>

        fun addInitialConstraint(initialConstraint: InitialConstraint)
        fun addError(error: KotlinCallDiagnostic)
    }

    fun addInitialSubtypeConstraint(c: Context, lowerType: KotlinTypeMarker, upperType: KotlinTypeMarker, position: ConstraintPosition) {
        val initialConstraint = InitialConstraint(lowerType, upperType, UPPER, position)
        c.addInitialConstraint(initialConstraint)
        updateAllowedTypeDepth(c, lowerType)
        updateAllowedTypeDepth(c, upperType)
        addSubTypeConstraintAndIncorporateIt(c, lowerType, upperType, IncorporationConstraintPosition(position, initialConstraint))
    }

    fun addInitialEqualityConstraint(c: Context, a: KotlinTypeMarker, b: KotlinTypeMarker, position: ConstraintPosition) {
        val initialConstraint = InitialConstraint(a, b, ConstraintKind.EQUALITY, position)
        c.addInitialConstraint(initialConstraint)
        updateAllowedTypeDepth(c, a)
        updateAllowedTypeDepth(c, b)
        addSubTypeConstraintAndIncorporateIt(c, a, b, IncorporationConstraintPosition(position, initialConstraint))
        addSubTypeConstraintAndIncorporateIt(c, b, a, IncorporationConstraintPosition(position, initialConstraint))
    }

    private fun addSubTypeConstraintAndIncorporateIt(
        c: Context,
        lowerType: KotlinTypeMarker,
        upperType: KotlinTypeMarker,
        incorporatePosition: IncorporationConstraintPosition
    ) {
        val possibleNewConstraints = Stack<Pair<TypeVariableMarker, Constraint>>()
        val typeCheckerContext = TypeCheckerContext(c, incorporatePosition, lowerType, upperType, possibleNewConstraints)
        typeCheckerContext.runIsSubtypeOf(lowerType, upperType)

        while (possibleNewConstraints.isNotEmpty()) {
            val (typeVariable, constraint) = possibleNewConstraints.pop()
            if (c.shouldWeSkipConstraint(typeVariable, constraint)) continue

            val constraints =
                c.notFixedTypeVariables[typeVariable.freshTypeConstructor(c)] ?: typeCheckerContext.fixedTypeVariable(typeVariable)

            // it is important, that we add constraint here(not inside TypeCheckerContext), because inside incorporation we read constraints
            constraints.addConstraint(constraint)?.let {
                constraintIncorporator.incorporate(typeCheckerContext, typeVariable, it)
            }
        }
    }

    private fun updateAllowedTypeDepth(c: Context, initialType: KotlinTypeMarker) = with(c) {
        c.maxTypeDepthFromInitialConstraints = max(c.maxTypeDepthFromInitialConstraints, initialType.typeDepth())
    }

    private fun Context.shouldWeSkipConstraint(typeVariable: TypeVariableMarker, constraint: Constraint): Boolean {
        assert(constraint.kind != ConstraintKind.EQUALITY)

        val constraintType = constraint.type
        if (!isAllowedType(constraintType)) return true

        if (constraintType.typeConstructor() == typeVariable.freshTypeConstructor()) {
            if (constraintType.lowerBoundIfFlexible().isMarkedNullable() && constraint.kind == LOWER) return false // T? <: T

            return true // T <: T(?!)
        }

        if (constraint.position.from is DeclaredUpperBoundConstraintPosition &&
            constraint.kind == UPPER && constraintType.isNullableAny()
        ) {
            return true // T <: Any?
        }

        return false
    }

    private fun Context.isAllowedType(type: KotlinTypeMarker) =
        type.typeDepth() <= maxTypeDepthFromInitialConstraints + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION

    private inner class TypeCheckerContext(
        val c: Context,
        val position: IncorporationConstraintPosition,
        val baseLowerType: KotlinTypeMarker,
        val baseUpperType: KotlinTypeMarker,
        val possibleNewConstraints: MutableList<Pair<TypeVariableMarker, Constraint>>
    ) : AbstractTypeCheckerContextForConstraintSystem(), ConstraintIncorporator.Context, TypeSystemInferenceExtensionContext by c {

        val baseContext: AbstractTypeCheckerContext = newBaseTypeCheckerContext(isErrorTypeEqualsToAnything, isStubTypeEqualsToAnything)

        override fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy {
            return baseContext.substitutionSupertypePolicy(type)
        }

        override fun areEqualTypeConstructors(a: TypeConstructorMarker, b: TypeConstructorMarker): Boolean {
            return baseContext.areEqualTypeConstructors(a, b)
        }

        override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
            return baseContext.prepareType(type)
        }

        @UseExperimental(TypeRefinement::class)
        override fun refineType(type: KotlinTypeMarker): KotlinTypeMarker {
            return if (type is KotlinType) {
                kotlinTypeRefiner.refineType(type)
            } else {
                type
            }
        }

        fun runIsSubtypeOf(lowerType: KotlinTypeMarker, upperType: KotlinTypeMarker) {
            if (!AbstractTypeChecker.isSubtypeOf(this@TypeCheckerContext as AbstractTypeCheckerContext, lowerType, upperType)) {
                // todo improve error reporting -- add information about base types
                c.addError(NewConstraintError(lowerType, upperType, position))
            }
        }

        // from AbstractTypeCheckerContextForConstraintSystem
        override fun isMyTypeVariable(type: SimpleTypeMarker): Boolean =
            type.mayBeTypeVariable() && c.allTypeVariables.containsKey(type.typeConstructor())

        override fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: KotlinTypeMarker) =
            addConstraint(typeVariable, superType, UPPER)

        override fun addLowerConstraint(typeVariable: TypeConstructorMarker, subType: KotlinTypeMarker) =
            addConstraint(typeVariable, subType, LOWER)

        private fun isCapturedTypeFromSubtyping(type: KotlinTypeMarker) =
            when ((type as? CapturedTypeMarker)?.captureStatus()) {
                null, CaptureStatus.FROM_EXPRESSION -> false
                CaptureStatus.FOR_SUBTYPING -> true
                CaptureStatus.FOR_INCORPORATION ->
                    error("Captured type for incorporation shouldn't escape from incorporation: $type\n" + renderBaseConstraint())
            }

        private fun addConstraint(typeVariableConstructor: TypeConstructorMarker, type: KotlinTypeMarker, kind: ConstraintKind) {
            val typeVariable = c.allTypeVariables[typeVariableConstructor]
                ?: error("Should by type variableConstructor: $typeVariableConstructor. ${c.allTypeVariables.values}")

            addNewIncorporatedConstraint(typeVariable, type, ConstraintContext(kind, emptySet(), isNullabilityConstraint = false))
        }

        // from ConstraintIncorporator.Context
        override fun addNewIncorporatedConstraint(lowerType: KotlinTypeMarker, upperType: KotlinTypeMarker) {
            if (lowerType === upperType) return
            if (c.isAllowedType(lowerType) && c.isAllowedType(upperType)) {
                runIsSubtypeOf(lowerType, upperType)
            }
        }

        override fun addNewIncorporatedConstraint(
            typeVariable: TypeVariableMarker,
            type: KotlinTypeMarker,
            constraintContext: ConstraintContext
        ) {
            val (kind, derivedFrom, inputTypePosition, isNullabilityConstraint) = constraintContext

            var targetType = type
            if (targetType.isUninferredParameter()) {
                // there already should be an error, so there is no point in reporting one more
                return
            }

            if (targetType.isError()) {
                c.addError(ConstrainingTypeIsError(typeVariable, targetType, position))
                return
            }

            if (type.contains(this::isCapturedTypeFromSubtyping)) {
                // TypeVariable <: type -> if TypeVariable <: subType => TypeVariable <: type
                if (kind == UPPER) {
                    val subType =
                        typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation)
                    if (subType != null) {
                        targetType = subType
                    }
                }

                if (kind == LOWER) {
                    val superType =
                        typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation)
                    if (superType != null) { // todo rethink error reporting for Any cases
                        targetType = superType
                    }
                }

                if (targetType === type) {
                    c.addError(CapturedTypeFromSubtyping(typeVariable, type, position))
                    return
                }
            }

            val newConstraint = Constraint(
                kind, targetType, position,
                derivedFrom = derivedFrom,
                isNullabilityConstraint = isNullabilityConstraint,
                inputTypePositionBeforeIncorporation = inputTypePosition
            )
            possibleNewConstraints.add(typeVariable to newConstraint)
        }

        override val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>
            get() = c.notFixedTypeVariables.values

        override fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker? {
            val typeVariable = c.allTypeVariables[typeConstructor]
            if (typeVariable != null && !c.notFixedTypeVariables.containsKey(typeConstructor)) {
                fixedTypeVariable(typeVariable)
            }
            return typeVariable
        }

        override fun getConstraintsForVariable(typeVariable: TypeVariableMarker) =
            c.notFixedTypeVariables[typeVariable.freshTypeConstructor()]?.constraints
                    ?: fixedTypeVariable(typeVariable)

        fun fixedTypeVariable(variable: TypeVariableMarker): Nothing {
            error(
                "Type variable $variable should not be fixed!\n" +
                        renderBaseConstraint()
            )
        }

        private fun renderBaseConstraint() = "Base constraint: $baseLowerType <: $baseUpperType from position: $position"
    }
}

data class ConstraintContext(
    val kind: ConstraintKind,
    val derivedFrom: Set<TypeVariableMarker>,
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null,
    val isNullabilityConstraint: Boolean
)
