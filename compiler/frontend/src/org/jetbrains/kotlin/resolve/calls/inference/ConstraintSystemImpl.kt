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

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl.ConstraintKind.EQUAL
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl.ConstraintKind.SUB_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.TYPE_BOUND_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.derivedFrom
import org.jetbrains.kotlin.resolve.descriptorUtil.hasInternalAnnotationForResolve
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.isInternalAnnotationForResolve
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.getNestedArguments
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.util.*

internal class ConstraintSystemImpl(
        private val allTypeParameterBounds: Map<TypeParameterDescriptor, TypeBoundsImpl>,
        private val externalTypeParameters: Set<TypeParameterDescriptor>,
        private val usedInBounds: Map<TypeParameterDescriptor, MutableList<TypeBounds.Bound>>,
        private val errors: List<ConstraintError>,
        private val initialConstraints: List<ConstraintSystemBuilderImpl.Constraint>,
        private val descriptorToVariable: Map<TypeParameterDescriptor, TypeParameterDescriptor>,
        private val variableToDescriptor: Map<TypeParameterDescriptor, TypeParameterDescriptor>
) : ConstraintSystem {
    private val localTypeParameterBounds: Map<TypeParameterDescriptor, TypeBoundsImpl>
        get() = if (externalTypeParameters.isEmpty()) allTypeParameterBounds
        else allTypeParameterBounds.filter { !externalTypeParameters.contains(it.key) }

    override val status = object : ConstraintSystemStatus {
        // for debug ConstraintsUtil.getDebugMessageForStatus might be used

        override fun isSuccessful() = !hasContradiction() && !hasUnknownParameters()

        override fun hasContradiction() = hasParameterConstraintError() || hasConflictingConstraints()
                                          || hasCannotCaptureTypesError() || hasTypeInferenceIncorporationError()

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
            typeParameterBounds: Map<TypeParameterDescriptor, TypeBoundsImpl>,
            getDefaultType: (TypeParameterDescriptor) -> KotlinType,
            substituteOriginal: Boolean
    ): Map<TypeParameterDescriptor, TypeProjection> {
        val substitutionContext = HashMap<TypeParameterDescriptor, TypeProjection>()
        for ((variable, typeBounds) in typeParameterBounds) {
            val value = typeBounds.value
            val typeParameter = if (substituteOriginal) variableToDescriptor[variable]!! else variable
            val type =
                    if (value != null && !TypeUtils.containsSpecialType(value, DONT_CARE)) value
                    else getDefaultType(typeParameter)
            substitutionContext.put(typeParameter, TypeProjectionImpl(type))
        }
        return substitutionContext
    }

    private fun replaceUninferredBy(
            getDefaultValue: (TypeParameterDescriptor) -> KotlinType,
            substituteOriginal: Boolean
    ): TypeSubstitutor {
        val parameterToInferredValueMap = getParameterToInferredValueMap(allTypeParameterBounds, getDefaultValue, substituteOriginal)
        val substitution = TypeConstructorSubstitution.createByParametersMap(parameterToInferredValueMap)
        return SubstitutionFilteringInternalResolveAnnotations(substitution).buildSubstitutor()
    }

    override fun getNestedTypeVariables(type: KotlinType): List<TypeParameterDescriptor> {
        return type.getNestedArguments().map { typeProjection ->
            typeProjection.type.constructor.declarationDescriptor as? TypeParameterDescriptor
        }.filterNotNull().filter { it in typeParameterDescriptors }
    }

    override val typeParameterDescriptors: Set<TypeParameterDescriptor>
        get() = descriptorToVariable.keys

    override val typeVariables: Set<TypeParameterDescriptor>
        get() = variableToDescriptor.keys

    override fun getTypeBounds(descriptor: TypeParameterDescriptor): TypeBoundsImpl {
        val variable = descriptorToVariable[descriptor]
        if (variable != null && variable != descriptor) {
            return getTypeBounds(variable)
        }
        return allTypeParameterBounds[descriptor] ?:
               throw IllegalArgumentException("TypeParameterDescriptor is not a type variable for constraint system: $descriptor")
    }

    override val resultingSubstitutor: TypeSubstitutor
        get() = getSubstitutor(substituteOriginal = true) { ErrorUtils.createUninferredParameterType(it) }

    override val currentSubstitutor: TypeSubstitutor
        get() = getSubstitutor(substituteOriginal = true) { TypeUtils.DONT_CARE }

    private fun getSubstitutor(substituteOriginal: Boolean, getDefaultValue: (TypeParameterDescriptor) -> KotlinType) =
            replaceUninferredBy(getDefaultValue, substituteOriginal).run {
                TypeSubstitutor.create(SubstitutionWithCapturedTypeApproximation(this.substitution))
            }

    private class SubstitutionWithCapturedTypeApproximation(substitution: TypeSubstitution) : DelegatedTypeSubstitution(substitution) {
        override fun approximateCapturedTypes() = true
    }

    private fun satisfyInitialConstraints(): Boolean {
        fun KotlinType.substitute(): KotlinType? {
            val substitutor = getSubstitutor(substituteOriginal = false) { ErrorUtils.createUninferredParameterType(it) }
            return substitutor.substitute(this, Variance.INVARIANT) ?: return null
        }
        return initialConstraints.all {
            constraint ->
            val resultSubType = constraint.subtype.substitute()?.let {
                // the call might be done via safe access, so we check for notNullable receiver type;
                // 'unsafe call' error is reported otherwise later
                if (constraint.position.kind != ConstraintPositionKind.RECEIVER_POSITION) it else it.makeNotNullable()
            } ?: return false
            val resultSuperType = constraint.superType.substitute() ?: return false
            when (constraint.kind) {
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
            variable to bounds.filterTo(arrayListOf<TypeBounds.Bound>()) { filterConstraintPosition(it.position )}
        }.toMap())
        result.externalTypeParameters.addAll(externalTypeParameters )
        result.errors.addAll(errors.filter { filterConstraintPosition(it.constraintPosition) })

        result.initialConstraints.addAll(initialConstraints.filter { filterConstraintPosition(it.position) })
        result.descriptorToVariable.putAll(descriptorToVariable)
        result.variableToDescriptor.putAll(variableToDescriptor)

        return result
    }
}

internal class SubstitutionFilteringInternalResolveAnnotations(substitution: TypeSubstitution) : DelegatedTypeSubstitution(substitution) {
    override fun filterAnnotations(annotations: Annotations): Annotations {
        if (!annotations.hasInternalAnnotationForResolve()) return annotations
        return FilteredAnnotations(annotations) { !it.isInternalAnnotationForResolve() }
    }
}
