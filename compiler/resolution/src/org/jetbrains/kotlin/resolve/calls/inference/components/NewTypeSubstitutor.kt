/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.checker.intersectTypes
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker

interface NewTypeSubstitutor: TypeSubstitutorMarker {
    fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType?

    fun safeSubstitute(type: UnwrappedType): UnwrappedType =
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
                    ).inheritEnhancement(type)
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

            val innerType = capturedType.lowerType ?: capturedType.constructor.projection.type.unwrap()
            val substitutedInnerType = substitute(innerType, keepAnnotation, runCapturedChecks = false)

            if (substitutedInnerType != null) {
                if (innerType is StubType || substitutedInnerType is StubType) {
                    return NewCapturedType(
                        capturedType.captureStatus,
                        NewCapturedTypeConstructor(TypeProjectionImpl(typeConstructor.projection.projectionKind, substitutedInnerType)),
                        lowerType = if (capturedType.lowerType != null) substitutedInnerType else null
                    )
                } else {
                    throwExceptionAboutInvalidCapturedSubstitution(capturedType, innerType, substitutedInnerType)
                }
            }

            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                typeConstructor.supertypes.forEach { supertype ->
                    substitute(supertype, keepAnnotation, runCapturedChecks = false)?.let {
                        throwExceptionAboutInvalidCapturedSubstitution(capturedType, supertype, it)
                    }
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
            replacement = replacement.replaceAnnotations(CompositeAnnotations(replacement.annotations, type.annotations))
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

    private fun throwExceptionAboutInvalidCapturedSubstitution(
        capturedType: SimpleType,
        innerType: UnwrappedType,
        substitutedInnerType: UnwrappedType
    ): Nothing =
        throw IllegalStateException(
            "Illegal type substitutor: $this, " +
                    "because for captured type '$capturedType' supertype approximation should be null, but it is: '$innerType'," +
                    "original supertype: '$substitutedInnerType'"
        )


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

object EmptySubstitutor : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? = null
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

fun createCompositeSubstitutor(appliedFirst: NewTypeSubstitutor, appliedLast: TypeSubstitutor): NewTypeSubstitutor {
    if (appliedLast.isEmpty) return appliedFirst

    return object : NewTypeSubstitutor {
        override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
            val substitutedOnce = appliedFirst.substituteNotNullTypeWithConstructor(constructor)

            return if (substitutedOnce != null) {
                appliedLast.substitute(substitutedOnce.unwrap())
            } else {
                constructor.declarationDescriptor?.defaultType?.let {
                    appliedLast.substitute(it)
                }
            }
        }
    }
}

fun NewTypeSubstitutor.composeWith(appliedAfter: TypeSubstitutor) = createCompositeSubstitutor(this, appliedAfter)
