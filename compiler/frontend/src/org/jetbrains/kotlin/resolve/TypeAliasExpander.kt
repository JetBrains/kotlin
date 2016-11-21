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
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.containsTypeAliasParameters
import org.jetbrains.kotlin.types.typeUtil.requiresTypeAliasExpansion

class TypeAliasExpander(
        private val reportStrategy: TypeAliasExpansionReportStrategy
) {
    fun expand(typeAliasExpansion: TypeAliasExpansion, annotations: Annotations) =
            expandRecursively(typeAliasExpansion, annotations, 0, true)

    fun expandWithoutAbbreviation(typeAliasExpansion: TypeAliasExpansion, annotations: Annotations) =
            expandRecursively(typeAliasExpansion, annotations, 0, false)

    private fun expandRecursively(
            typeAliasExpansion: TypeAliasExpansion,
            annotations: Annotations,
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

        val expandedTypeWithExtraAnnotations = expandedType.combineAnnotations(annotations)

        return if (withAbbreviatedType)
            expandedTypeWithExtraAnnotations.withAbbreviation(typeAliasExpansion.createAbbreviation(underlyingProjection, annotations))
        else
            expandedTypeWithExtraAnnotations
    }

    private fun SimpleType.combineAnnotations(annotations: Annotations): SimpleType {
        val existingAnnotationTypes = this.annotations.getAllAnnotations().mapTo(hashSetOf<KotlinType>()) { it.annotation.type }

        for (annotation in annotations) {
            if (annotation.type in existingAnnotationTypes) {
                reportStrategy.repeatedAnnotation(annotation)
            }
        }

        return replace(newAnnotations = CompositeAnnotations(listOf(annotations, this.annotations)))
    }

    private fun TypeAliasExpansion.createAbbreviation(originalProjection: TypeProjection, annotations: Annotations) =
            KotlinTypeFactory.simpleType(
                    annotations,
                    descriptor.typeConstructor,
                    arguments,
                    originalProjection.type.isMarkedNullable,
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
        val argument = typeAliasExpansion.getReplacement(underlyingType.constructor)
        if (argument == null) {
            return expandNonArgumentTypeProjection(underlyingProjection, typeAliasExpansion, recursionDepth)
        }

        if (argument.isStarProjection) return TypeUtils.makeStarProjection(typeParameterDescriptor!!)

        val argumentVariance = argument.projectionKind
        val underlyingVariance = underlyingProjection.projectionKind

        val argumentType = argument.type as? SimpleType ?: throw AssertionError("Non-simple type in type alias argument: $argument")

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
        val resultingVariance =
                when {
                    parameterVariance == substitutionVariance -> Variance.INVARIANT
                    parameterVariance == Variance.INVARIANT -> substitutionVariance
                    substitutionVariance == Variance.INVARIANT -> Variance.INVARIANT
                    else -> {
                        reportStrategy.conflictingProjection(typeAliasExpansion.descriptor, typeParameterDescriptor, argumentType)
                        substitutionVariance
                    }
                }

        val substitutedType = TypeUtils.makeNullableIfNeeded(argumentType, underlyingType.isMarkedNullable)
                .combineAnnotations(underlyingType.annotations)

        return TypeProjectionImpl(resultingVariance, substitutedType)
    }

    private fun expandNonArgumentTypeProjection(
            originalProjection: TypeProjection,
            typeAliasExpansion: TypeAliasExpansion,
            recursionDepth: Int
    ): TypeProjection {
        val type = originalProjection.type.asSimpleType()

        if (type.isError || !type.requiresTypeAliasExpansion()) {
            return originalProjection
        }

        val typeConstructor = type.constructor
        val typeDescriptor = typeConstructor.declarationDescriptor

        assert(typeConstructor.parameters.size == type.arguments.size) { "Unexpected malformed type: $type" }
        
        when (typeDescriptor) {
            is TypeParameterDescriptor -> {
                return originalProjection
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

                val expandedType = expandRecursively(nestedExpansion, type.annotations, recursionDepth + 1, false)

                // 'dynamic' type can't be abbreviated - will be reported separately
                val typeWithAbbreviation = if (expandedType.isDynamic()) expandedType else expandedType.withAbbreviation(type)

                return TypeProjectionImpl(originalProjection.projectionKind, typeWithAbbreviation)
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

                return TypeProjectionImpl(originalProjection.projectionKind, substitutedType)
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
