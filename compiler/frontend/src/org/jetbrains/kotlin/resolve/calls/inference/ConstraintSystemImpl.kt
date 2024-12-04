/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl.ConstraintKind.EQUAL
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl.ConstraintKind.SUB_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.TYPE_BOUND_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.derivedFrom
import org.jetbrains.kotlin.resolve.descriptorUtil.hasInternalAnnotationForResolve
import org.jetbrains.kotlin.resolve.descriptorUtil.isInternalAnnotationForResolve
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.util.*

internal class ConstraintSystemImpl(
    private val allTypeParameterBounds: Map<TypeVariable, TypeBoundsImpl>,
    private val usedInBounds: Map<TypeVariable, MutableList<TypeBounds.Bound>>,
    private val errors: List<ConstraintError>,
    private val initialConstraints: List<ConstraintSystemBuilderImpl.Constraint>,
    private val typeVariableSubstitutors: Map<CallHandle, TypeSubstitutor>
) : ConstraintSystem {
    private val localTypeParameterBounds: Map<TypeVariable, TypeBoundsImpl>
        get() = allTypeParameterBounds.filterNot { it.key.isExternal }

    override val status = object : ConstraintSystemStatus {
        // for debug ConstraintsUtil.getDebugMessageForStatus might be used

        override fun isSuccessful() = !hasContradiction() && !hasUnknownParameters() && satisfyInitialConstraints()

        override fun hasContradiction() = hasParameterConstraintError() || hasConflictingConstraints()
                || hasCannotCaptureTypesError() || errors.any { it is TypeInferenceError }

        /**
         * All hacks were removed. This comment is left for information.
         *
         * Hacks above are needed for the following example:
         *
         * @kotlin.jvm.JvmName("containsAny")
         * @kotlin.internal.LowPriorityInOverloadResolution
         * public operator fun <T> Iterable<T>.contains(element: T): Boolean
         *
         * public operator fun <@kotlin.internal.OnlyInputTypes T> Iterable<T>.contains(element: T): Boolean
         *
         * fun test() = listOf(1).contains("")
         *
         * When we resolve call `contains`, we should choose candidate before we complete inference.
         * Because of this we can't check OnlyInputTypes when we trying choose candidate.
         * Now we do this check in this moment, but it is incorrect and we should remove it later.
         *
         * Call !satisfyInitialConstraints() in hasTypeInferenceIncorporationError() is needed for this example:
         * @kotlin.jvm.JvmName("containsAny")
         * @kotlin.internal.LowPriorityInOverloadResolution
         * public operator fun <T> Iterable<T>.contains(element: T): Boolean
         *
         * public operator fun <T> Iterable<T>.contains(element: @kotlin.internal.NoInfer T)
         *
         * fun test() = listOf(1).contains("")
         *
         * It is also incorrect, because we can get additional constraints on T after we resolve call `contains`.
         */

        override fun hasViolatedUpperBound() = !isSuccessful() && filterConstraintsOut(TYPE_BOUND_POSITION).status.isSuccessful()

        override fun hasConflictingConstraints() = localTypeParameterBounds.values.any { it.values.size > 1 }

        override fun hasUnknownParameters() =
            localTypeParameterBounds.values.any { it.values.isEmpty() } || hasTypeParameterWithUnsatisfiedOnlyInputTypesError()

        override fun hasParameterConstraintError() = errors.any { it is ParameterConstraintError }

        override fun hasOnlyErrorsDerivedFrom(kind: ConstraintPositionKind): Boolean {
            if (isSuccessful()) return false
            if (filterConstraintsOut(kind).status.isSuccessful()) return true
            return errors.isNotEmpty() && errors.all { it.constraintPosition.derivedFrom(kind) }
        }

        override fun hasErrorInConstrainingTypes() = errors.any { it is ErrorInConstrainingType }

        override fun hasCannotCaptureTypesError() = errors.any { it is CannotCapture }

        override fun hasTypeInferenceIncorporationError() = errors.any { it is TypeInferenceError } || !satisfyInitialConstraints()

        override fun hasTypeParameterWithUnsatisfiedOnlyInputTypesError() =
            localTypeParameterBounds.values.any { it.typeVariable.hasOnlyInputTypesAnnotation() && it.value == null }

        override val constraintErrors: List<ConstraintError>
            get() = errors
    }

    private fun getParameterToInferredValueMap(
        typeParameterBounds: Map<TypeVariable, TypeBoundsImpl>,
        getDefaultType: (TypeVariable) -> KotlinType,
        substituteOriginal: Boolean
    ): Map<TypeConstructor, TypeProjection> {
        val substitutionContext = HashMap<TypeConstructor, TypeProjection>()
        for ((variable, typeBounds) in typeParameterBounds) {
            val value = typeBounds.value
            val typeConstructor =
                if (substituteOriginal) variable.originalTypeParameter.typeConstructor
                else variable.type.constructor
            val type =
                if (value != null && !TypeUtils.contains(value, DONT_CARE)) value
                else getDefaultType(variable)
            substitutionContext.put(typeConstructor, TypeProjectionImpl(type))
        }
        return substitutionContext
    }

    override val typeVariables: Set<TypeVariable>
        get() = allTypeParameterBounds.keys

    override fun getTypeBounds(typeVariable: TypeVariable): TypeBoundsImpl {
        return allTypeParameterBounds[typeVariable]
                ?: throw IllegalArgumentException("TypeParameterDescriptor is not a type variable for constraint system: $typeVariable")
    }

    override val resultingSubstitutor: TypeSubstitutor
        get() = getSubstitutor(substituteOriginal = true) {
            ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_TYPE_VARIABLE, it.originalTypeParameter.name.asString())
        }

    override val currentSubstitutor: TypeSubstitutor
        get() = getSubstitutor(substituteOriginal = true) { DONT_CARE }

    private fun getSubstitutor(substituteOriginal: Boolean, getDefaultValue: (TypeVariable) -> KotlinType): TypeSubstitutor {
        val parameterToInferredValueMap = getParameterToInferredValueMap(allTypeParameterBounds, getDefaultValue, substituteOriginal)
        return TypeSubstitutor.create(
            SubstitutionWithCapturedTypeApproximation(
                SubstitutionFilteringInternalResolveAnnotations(
                    TypeConstructorSubstitution.createByConstructorsMap(parameterToInferredValueMap)
                )
            )
        )
    }

    private fun satisfyInitialConstraints(): Boolean {
        val substitutor = getSubstitutor(substituteOriginal = false) {
            ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_TYPE_VARIABLE, it.originalTypeParameter.name.asString())
        }
        fun KotlinType.substitute(): KotlinType? = substitutor.substitute(this, Variance.INVARIANT)

        return initialConstraints.all { (kind, subtype, superType, position) ->
            val resultSubType = subtype.substitute()?.let {
                // the call might be done via safe access, so we check for notNullable receiver type;
                // 'unsafe call' error is reported otherwise later
                if (position.kind != ConstraintPositionKind.RECEIVER_POSITION) it else it.makeNotNullable()
            } ?: return false
            val resultSuperType = superType.substitute() ?: return false
            when (kind) {
                SUB_TYPE -> KotlinTypeChecker.DEFAULT.isSubtypeOf(resultSubType, resultSuperType)
                EQUAL -> KotlinTypeChecker.DEFAULT.equalTypes(resultSubType, resultSuperType)
            }
        }
    }

    override fun toBuilder(filterConstraintPosition: (ConstraintPosition) -> Boolean): ConstraintSystem.Builder {
        val result = ConstraintSystemBuilderImpl()
        for ((typeParameter, typeBounds) in allTypeParameterBounds) {
            result.allTypeParameterBounds.put(typeParameter, typeBounds.filter(filterConstraintPosition))
        }
        result.usedInBounds.putAll(usedInBounds.map {
            val (variable, bounds) = it
            variable to bounds.filterTo(arrayListOf<TypeBounds.Bound>()) { filterConstraintPosition(it.position) }
        }.toMap())
        result.errors.addAll(errors.filter { filterConstraintPosition(it.constraintPosition) })

        result.initialConstraints.addAll(initialConstraints.filter { filterConstraintPosition(it.position) })
        result.typeVariableSubstitutors.putAll(typeVariableSubstitutors)

        return result
    }
}

internal class SubstitutionFilteringInternalResolveAnnotations(substitution: TypeSubstitution) : DelegatedTypeSubstitution(substitution) {
    override fun filterAnnotations(annotations: Annotations): Annotations {
        if (!annotations.hasInternalAnnotationForResolve()) return annotations
        return FilteredAnnotations(annotations) { !it.isInternalAnnotationForResolve() }
    }
}
