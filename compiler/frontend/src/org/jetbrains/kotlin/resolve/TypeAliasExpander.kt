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
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*

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
    ): KotlinType {
        val originalProjection = TypeProjectionImpl(Variance.INVARIANT, typeAliasExpansion.descriptor.underlyingType)
        val expandedProjection = expandTypeProjection(originalProjection, typeAliasExpansion, null, recursionDepth)
        val expandedType = expandedProjection.type

        if (expandedType.isError) return expandedType

        assert(expandedProjection.projectionKind == Variance.INVARIANT) {
            "Type alias expansion: result for ${typeAliasExpansion.descriptor} is ${expandedProjection.projectionKind}, should be invariant"
        }

        return if (withAbbreviatedType) {
            val abbreviatedType = KotlinTypeImpl.create(annotations,
                                                        typeAliasExpansion.descriptor.typeConstructor,
                                                        originalProjection.type.isMarkedNullable,
                                                        typeAliasExpansion.arguments,
                                                        MemberScope.Empty)

            expandedType.withAbbreviatedType(abbreviatedType)
        }
        else {
            expandedType
        }
    }

    private fun expandTypeProjection(
            originalProjection: TypeProjection,
            typeAliasExpansion: TypeAliasExpansion,
            typeParameterDescriptor: TypeParameterDescriptor?,
            recursionDepth: Int
    ): TypeProjection {
        assertRecursionDepth(recursionDepth, typeAliasExpansion.descriptor)

        val originalType = originalProjection.type

        val typeAliasArgument = typeAliasExpansion.getReplacement(originalType.constructor)

        if (typeAliasArgument == null) {
            return expandNonArgumentTypeProjection(originalProjection, typeAliasExpansion, recursionDepth)
        }

        val originalVariance =
                if (originalProjection.projectionKind != Variance.INVARIANT)
                    originalProjection.projectionKind
                else if (typeParameterDescriptor != null)
                    typeParameterDescriptor.variance
                else
                    Variance.INVARIANT

        val argumentVariance = typeAliasArgument.projectionKind

        val substitutedVariance =
                if (argumentVariance == Variance.INVARIANT)
                    originalVariance
                else if (originalVariance == Variance.INVARIANT || originalVariance == argumentVariance)
                    argumentVariance
                else if (typeAliasArgument.isStarProjection)
                    argumentVariance
                else {
                    if (originalVariance != argumentVariance && !typeAliasArgument.isStarProjection) {
                        reportStrategy.conflictingProjection(typeAliasExpansion.descriptor, typeParameterDescriptor, typeAliasArgument.type)
                    }
                    argumentVariance
                }

        val substitutedType = TypeUtils.makeNullableIfNeeded(typeAliasArgument.type, originalType.isMarkedNullable)

        return TypeProjectionImpl(substitutedVariance, substitutedType)
    }

    private fun expandNonArgumentTypeProjection(
            originalProjection: TypeProjection,
            typeAliasExpansion: TypeAliasExpansion,
            recursionDepth: Int
    ): TypeProjection {
        val type = originalProjection.type

        if (!type.requiresTypeAliasExpansion()) {
            return originalProjection
        }

        val typeConstructor = type.constructor
        val typeDescriptor = typeConstructor.declarationDescriptor

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
                val typeWithAbbreviation = if (expandedType.isDynamic()) expandedType else expandedType.withAbbreviatedType(type)

                return TypeProjectionImpl(originalProjection.projectionKind, typeWithAbbreviation)
            }
            else -> {
                val substitutedArguments = type.arguments.mapIndexed { i, originalArgument ->
                    expandTypeProjection(originalArgument, typeAliasExpansion, typeConstructor.parameters[i], recursionDepth + 1)
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
            if (!substitutedArgument.type.dependsOnTypeAliasParameters()) {
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
