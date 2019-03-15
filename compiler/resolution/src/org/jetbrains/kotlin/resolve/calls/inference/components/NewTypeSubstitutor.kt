/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.checker.intersectTypes

interface NewTypeSubstitutor {
    fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType?

    fun safeSubstitute(type: UnwrappedType): UnwrappedType = substitute(type, runCapturedChecks = true, keepAnnotation = false) ?: type

    fun substituteKeepAnnotations(type: UnwrappedType): UnwrappedType =
        substitute(type, runCapturedChecks = true, keepAnnotation = true) ?: type

    private fun substitute(type: UnwrappedType, keepAnnotation: Boolean, runCapturedChecks: Boolean): UnwrappedType? =
        when (type) {
            is SimpleType -> substitute(type, keepAnnotation, runCapturedChecks)
            is FlexibleType -> if (type is DynamicType || type is RawType) {
                null
            } else {
                val lowerBound = substitute(type.lowerBound, keepAnnotation, runCapturedChecks)
                val upperBound = substitute(type.upperBound, keepAnnotation, runCapturedChecks)
                if (lowerBound == null && upperBound == null) {
                    null
                } else {
                    // todo discuss lowerIfFlexible and upperIfFlexible
                    KotlinTypeFactory.flexibleType(
                        lowerBound?.lowerIfFlexible() ?: type.lowerBound,
                        upperBound?.upperIfFlexible() ?: type.upperBound
                    )
                }
            }
        }

    private fun substitute(type: SimpleType, keepAnnotation: Boolean, runCapturedChecks: Boolean): UnwrappedType? {
        if (type.isError) return null

        if (type is AbbreviatedType) {
            val substitutedExpandedType = substitute(type.expandedType, keepAnnotation, runCapturedChecks)
            val substitutedAbbreviation = substitute(type.abbreviation, keepAnnotation, runCapturedChecks)
            return when {
                substitutedExpandedType == null && substitutedAbbreviation == null -> null
                substitutedExpandedType is SimpleType? && substitutedAbbreviation is SimpleType? ->
                    AbbreviatedType(
                        substitutedExpandedType ?: type.expandedType,
                        substitutedAbbreviation ?: type.abbreviation
                    )
                else -> substitutedExpandedType
            }
        }

        if (type.arguments.isNotEmpty()) {
            return substituteParametrizedType(type, keepAnnotation, runCapturedChecks)
        }

        val typeConstructor = type.constructor

        if (typeConstructor is NewCapturedTypeConstructor) {
            if (!runCapturedChecks) return null

            assert(type is NewCapturedType || (type is DefinitelyNotNullType && type.original is NewCapturedType)) {
                // KT-16147
                "Type is inconsistent -- somewhere we create type with typeConstructor = $typeConstructor " +
                        "and class: ${type::class.java.canonicalName}. type.toString() = $type"
            }
            val capturedType = if (type is DefinitelyNotNullType) type.original as NewCapturedType else type as NewCapturedType
            val lower = capturedType.lowerType?.let { substitute(it, keepAnnotation, runCapturedChecks = false) }
            if (lower != null) throw IllegalStateException(
                "Illegal type substitutor: $this, " +
                        "because for captured type '$type' lower type approximation should be null, but it is: '$lower'," +
                        "original lower type: '${capturedType.lowerType}"
            )

            typeConstructor.supertypes.forEach { supertype ->
                substitute(supertype, keepAnnotation, runCapturedChecks = false)?.let {
                    throw IllegalStateException(
                        "Illegal type substitutor: $this, " +
                                "because for captured type '$type' supertype approximation should be null, but it is: '$supertype'," +
                                "original supertype: '$supertype'"
                    )
                }
            }

            return null
        }

        if (typeConstructor is IntersectionTypeConstructor) {
            var thereIsChanges = false
            val newTypes = typeConstructor.supertypes.map {
                substitute(it.unwrap(), keepAnnotation, runCapturedChecks)?.apply { thereIsChanges = true } ?: it.unwrap()
            }
            if (!thereIsChanges) return null
            return intersectTypes(newTypes).let { if (type.isMarkedNullable) it.makeNullableAsSpecified(true) else it }
        }

        // simple classifier type
        var replacement = substituteNotNullTypeWithConstructor(typeConstructor) ?: return null
        if (keepAnnotation) {
            replacement = replacement.replaceAnnotations(type.annotations)
        }
        if (type.isMarkedNullable) {
            replacement = replacement.makeNullableAsSpecified(true)
        }
        if (type.isDefinitelyNotNullType) {
            replacement = replacement.makeDefinitelyNotNullOrNotNull()
        }
        if (type is CustomTypeVariable) {
            replacement = type.substitutionResult(replacement).unwrap()
        }

        return replacement
    }

    private fun substituteParametrizedType(
        type: SimpleType,
        keepAnnotation: Boolean,
        runCapturedChecks: Boolean
    ): UnwrappedType? {
        val parameters = type.constructor.parameters
        val arguments = type.arguments
        if (parameters.size != arguments.size) {
            // todo error type or exception?
            return ErrorUtils.createErrorType("Inconsistent type: $type (parameters.size = ${parameters.size}, arguments.size = ${arguments.size})")
        }
        val newArguments = arrayOfNulls<TypeProjection?>(arguments.size)

        for (index in arguments.indices) {
            val argument = arguments[index]

            if (argument.isStarProjection) continue
            val substitutedArgumentType = substitute(argument.type.unwrap(), keepAnnotation, runCapturedChecks) ?: continue

            newArguments[index] = TypeProjectionImpl(argument.projectionKind, substitutedArgumentType)
        }

        if (newArguments.all { it == null }) return null

        val newArgumentsList = arguments.mapIndexed { index, oldArgument -> newArguments[index] ?: oldArgument }
        return type.replace(newArgumentsList)
    }
}

class NewTypeSubstitutorByConstructorMap(val map: Map<TypeConstructor, UnwrappedType>) : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? = map[constructor]
}

class FreshVariableNewTypeSubstitutor(val freshVariables: List<TypeVariableFromCallableDescriptor>) : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
        val indexProposal = (constructor.declarationDescriptor as? TypeParameterDescriptor)?.index ?: return null
        val typeVariable = freshVariables.getOrNull(indexProposal) ?: return null
        if (typeVariable.originalTypeParameter.typeConstructor != constructor) return null

        return typeVariable.defaultType
    }

    companion object {
        val Empty = FreshVariableNewTypeSubstitutor(emptyList())
    }
}

fun UnwrappedType.substituteTypeVariable(typeVariable: NewTypeVariable, value: UnwrappedType): UnwrappedType {
    val substitutor = NewTypeSubstitutorByConstructorMap(mapOf(typeVariable.freshTypeConstructor to value))
    return substitutor.safeSubstitute(this)
}