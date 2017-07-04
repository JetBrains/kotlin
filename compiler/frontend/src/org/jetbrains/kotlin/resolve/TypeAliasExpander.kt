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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.composeAnnotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.containsTypeAliasParameters
import org.jetbrains.kotlin.types.typeUtil.requiresTypeAliasExpansion

class TypeAliasExpander(
        private val reportStrategy: TypeAliasExpansionReportStrategy
) {
    fun expand(typeAliasExpansion: TypeAliasExpansion, annotations: Annotations) =
            expandRecursively(typeAliasExpansion, annotations,
                              isNullable = false, recursionDepth = 0, withAbbreviatedType = true)

    fun expandWithoutAbbreviation(typeAliasExpansion: TypeAliasExpansion, annotations: Annotations) =
            expandRecursively(typeAliasExpansion, annotations,
                              isNullable = false, recursionDepth = 0, withAbbreviatedType = false)

    private fun expandRecursively(
            typeAliasExpansion: TypeAliasExpansion,
            annotations: Annotations,
            isNullable: Boolean,
            recursionDepth: Int,
            withAbbreviatedType: Boolean
    ): SimpleType {
        val underlyingProjection = TypeProjectionImpl(Variance.INVARIANT, typeAliasExpansion.descriptor.underlyingType)
        val expandedProjection = expandTypeProjection(underlyingProjection, typeAliasExpansion, null, recursionDepth)
        val expandedType = expandedProjection.type.asSimpleType()

        if (expandedType.isError) return expandedType

        assert(expandedProjection.projectionKind == Variance.INVARIANT) {
            "Type alias expansion: result for ${typeAliasExpansion.descriptor} is ${expandedProjection.projectionKind}, should be invariant"
        }

        checkRepeatedAnnotations(expandedType.annotations, annotations)
        val expandedTypeWithExtraAnnotations = expandedType.combineAnnotations(annotations).let { TypeUtils.makeNullableIfNeeded(it, isNullable) }

        return if (withAbbreviatedType)
            expandedTypeWithExtraAnnotations.withAbbreviation(typeAliasExpansion.createAbbreviation(annotations, isNullable))
        else
            expandedTypeWithExtraAnnotations
    }

    private fun TypeAliasExpansion.createAbbreviation(annotations: Annotations, isNullable: Boolean) =
            KotlinTypeFactory.simpleType(
                    annotations,
                    descriptor.typeConstructor,
                    arguments,
                    isNullable,
                    MemberScope.Empty
            )

    private fun expandTypeProjection(
            underlyingProjection: TypeProjection,
            typeAliasExpansion: TypeAliasExpansion,
            typeParameterDescriptor: TypeParameterDescriptor?,
            recursionDepth: Int
    ): TypeProjection {
        // TODO refactor TypeSubstitutor to introduce custom diagnostics
        assertRecursionDepth(recursionDepth, typeAliasExpansion.descriptor)

        if (underlyingProjection.isStarProjection) return TypeUtils.makeStarProjection(typeParameterDescriptor!!)

        val underlyingType = underlyingProjection.type
        val argument = typeAliasExpansion.getReplacement(underlyingType.constructor) ?:
                       return expandNonArgumentTypeProjection(underlyingProjection, typeAliasExpansion, recursionDepth)

        if (argument.isStarProjection) return TypeUtils.makeStarProjection(typeParameterDescriptor!!)

        val argumentType = argument.type.unwrap()

        val resultingVariance = run {
            val argumentVariance = argument.projectionKind
            val underlyingVariance = underlyingProjection.projectionKind

            val substitutionVariance =
                    when {
                        underlyingVariance == argumentVariance -> argumentVariance
                        underlyingVariance == Variance.INVARIANT -> argumentVariance
                        argumentVariance == Variance.INVARIANT -> underlyingVariance
                        else -> {
                            reportStrategy.conflictingProjection(typeAliasExpansion.descriptor, typeParameterDescriptor, argumentType)
                            argumentVariance
                        }
                    }

            val parameterVariance = typeParameterDescriptor?.variance ?: Variance.INVARIANT

            when {
                parameterVariance == substitutionVariance -> substitutionVariance
                parameterVariance == Variance.INVARIANT -> substitutionVariance
                substitutionVariance == Variance.INVARIANT -> Variance.INVARIANT
                else -> {
                    reportStrategy.conflictingProjection(typeAliasExpansion.descriptor, typeParameterDescriptor, argumentType)
                    substitutionVariance
                }
            }
        }

        checkRepeatedAnnotations(underlyingType.annotations, argumentType.annotations)

        val substitutedType =
                if (argumentType is DynamicType)
                    argumentType.combineAnnotations(underlyingType.annotations)
                else
                    argumentType.asSimpleType().combineNullabilityAndAnnotations(underlyingType)

        return TypeProjectionImpl(resultingVariance, substitutedType)
    }

    private fun DynamicType.combineAnnotations(newAnnotations: Annotations): DynamicType =
            replaceAnnotations(createCombinedAnnotations(newAnnotations))

    private fun SimpleType.combineAnnotations(newAnnotations: Annotations): SimpleType =
            if (isError) this else replace(newAnnotations = createCombinedAnnotations(newAnnotations))

    private fun KotlinType.createCombinedAnnotations(newAnnotations: Annotations): Annotations {
        if (isError) return annotations

        return composeAnnotations(newAnnotations, annotations)
    }

    private fun checkRepeatedAnnotations(existingAnnotations: Annotations, newAnnotations: Annotations) {
        val existingAnnotationFqNames = existingAnnotations.mapTo(hashSetOf()) { it.fqName }

        for (annotation in newAnnotations) {
            if (annotation.fqName in existingAnnotationFqNames) {
                reportStrategy.repeatedAnnotation(annotation)
            }
        }
    }

    private fun SimpleType.combineNullability(fromType: KotlinType) =
            TypeUtils.makeNullableIfNeeded(this, fromType.isMarkedNullable)

    private fun SimpleType.combineNullabilityAndAnnotations(fromType: KotlinType) =
            combineNullability(fromType).combineAnnotations(fromType.annotations)

    private fun expandNonArgumentTypeProjection(
            originalProjection: TypeProjection,
            typeAliasExpansion: TypeAliasExpansion,
            recursionDepth: Int
    ): TypeProjection {
        val originalType = originalProjection.type.unwrap()

        if (originalType.isDynamic()) return originalProjection

        val type = originalType.asSimpleType()

        if (type.isError || !type.requiresTypeAliasExpansion()) {
            return originalProjection
        }

        val typeConstructor = type.constructor
        val typeDescriptor = typeConstructor.declarationDescriptor

        assert(typeConstructor.parameters.size == type.arguments.size) { "Unexpected malformed type: $type" }

        return when (typeDescriptor) {
            is TypeParameterDescriptor -> {
                originalProjection
            }
            is TypeAliasDescriptor -> {
                if (typeAliasExpansion.isRecursion(typeDescriptor)) {
                    reportStrategy.recursiveTypeAlias(typeDescriptor)
                    return TypeProjectionImpl(Variance.INVARIANT, ErrorUtils.createErrorType("Recursive type alias: ${typeDescriptor.name}"))
                }

                val expandedArguments = type.arguments.mapIndexed { i, typeAliasArgument ->
                    expandTypeProjection(typeAliasArgument, typeAliasExpansion, typeConstructor.parameters[i], recursionDepth + 1)
                }

                val nestedExpansion = TypeAliasExpansion.create(typeAliasExpansion, typeDescriptor, expandedArguments)

                val nestedExpandedType = expandRecursively(nestedExpansion, type.annotations,
                                                           isNullable = type.isMarkedNullable,
                                                           recursionDepth = recursionDepth + 1,
                                                           withAbbreviatedType = false)

                // 'dynamic' type can't be abbreviated - will be reported separately
                val typeWithAbbreviation = if (nestedExpandedType.isDynamic()) nestedExpandedType else nestedExpandedType.withAbbreviation(type)

                TypeProjectionImpl(originalProjection.projectionKind, typeWithAbbreviation)
            }
            else -> {
                val substitutedArguments = type.arguments.mapIndexed { i, originalArgument ->
                    val projection = expandTypeProjection(
                            originalArgument, typeAliasExpansion, typeConstructor.parameters[i], recursionDepth + 1)
                    if (projection.isStarProjection) projection
                    else TypeProjectionImpl(projection.projectionKind,
                                            TypeUtils.makeNullableIfNeeded(projection.type, originalArgument.type.isMarkedNullable))
                }

                val substitutedType = type.replace(newArguments = substitutedArguments)

                checkTypeArgumentsSubstitution(type, substitutedType)

                TypeProjectionImpl(originalProjection.projectionKind, substitutedType)
            }
        }
    }

    private fun checkTypeArgumentsSubstitution(unsubstitutedType: KotlinType, substitutedType: KotlinType) {
        val typeSubstitutor = TypeSubstitutor.create(substitutedType)

        substitutedType.arguments.forEachIndexed { i, substitutedArgument ->
            if (!substitutedArgument.isStarProjection && !substitutedArgument.type.containsTypeAliasParameters()) {
                val unsubstitutedArgument = unsubstitutedType.arguments[i]
                val typeParameter = unsubstitutedType.constructor.parameters[i]
                DescriptorResolver.checkBoundsInTypeAlias(reportStrategy, unsubstitutedArgument.type, substitutedArgument.type, typeParameter, typeSubstitutor)
            }
        }
    }

    companion object {
        private const val MAX_RECURSION_DEPTH = 100

        private fun assertRecursionDepth(recursionDepth: Int, typeAliasDescriptor: TypeAliasDescriptor) {
            if (recursionDepth > MAX_RECURSION_DEPTH) {
                throw AssertionError("Too deep recursion while expanding type alias ${typeAliasDescriptor.name}")
            }
        }

        val NON_REPORTING = TypeAliasExpander(TypeAliasExpansionReportStrategy.DO_NOTHING)
    }
}
