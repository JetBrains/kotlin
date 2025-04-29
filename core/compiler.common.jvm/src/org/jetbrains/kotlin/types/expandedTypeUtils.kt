/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.model.*

fun TypeSystemCommonBackendContext.computeExpandedTypeForInlineClass(inlineClassType: KotlinTypeMarker): KotlinTypeMarker? =
    computeExpandedTypeInner(inlineClassType, hashSetOf())

private fun TypeSystemCommonBackendContext.computeExpandedTypeInner(
    kotlinType: KotlinTypeMarker, visitedClassifiers: HashSet<TypeConstructorMarker>
): KotlinTypeMarker? {
    val classifier = kotlinType.typeConstructor()
    if (!visitedClassifiers.add(classifier)) return null

    val typeParameter = classifier.getTypeParameterClassifier()

    return when {
        typeParameter != null -> {
            val upperBound = typeParameter.getRepresentativeUpperBound()
            computeExpandedTypeInner(upperBound, visitedClassifiers)
                ?.let { expandedUpperBound ->
                    val upperBoundIsPrimitiveOrInlineClass =
                        upperBound.typeConstructor().isInlineClass() || upperBound is SimpleTypeMarker && upperBound.isPrimitiveType()
                    when {
                        expandedUpperBound is SimpleTypeMarker && expandedUpperBound.isPrimitiveType() &&
                                kotlinType.isNullableType() && upperBoundIsPrimitiveOrInlineClass -> upperBound.makeNullable()
                        expandedUpperBound.isNullableType() || !kotlinType.isMarkedNullable() -> expandedUpperBound
                        else -> expandedUpperBound.makeNullable()
                    }
                }
        }

        classifier.isInlineClass() -> {
            val underlyingType = getSubstitutedUnderlyingType(kotlinType) ?: return null
            val expandedUnderlyingType = computeExpandedTypeInner(underlyingType, visitedClassifiers) ?: return null
            when {
                !kotlinType.isNullableType() -> expandedUnderlyingType

                // Here inline class type is nullable. Apply nullability to the expandedUnderlyingType.

                // Nullable types become inline class boxes
                expandedUnderlyingType.isNullableType() -> kotlinType

                // Primitives become inline class boxes
                expandedUnderlyingType is SimpleTypeMarker && expandedUnderlyingType.isPrimitiveType() -> kotlinType

                // Non-null reference types become nullable reference types
                else -> expandedUnderlyingType.makeNullable()
            }
        }

        else -> kotlinType
    }
}

private fun TypeSystemCommonBackendContext.getSubstitutedUnderlyingType(type: KotlinTypeMarker): KotlinTypeMarker? {
    val typeParameters = type.typeConstructor().getParameters()
    val typeArguments = type.getArguments().mapIndexed { index, typeArgument ->
        typeArgument.getType() ?: typeParameters[index].getRepresentativeUpperBound()
    }
    val mapping = typeParameters.map { it.getTypeConstructor() }.zip(typeArguments).toMap()
    val substitutor = typeSubstitutorForUnderlyingType(mapping)

    val underlyingType = type.getUnsubstitutedUnderlyingType() ?: return null
    return when (val underlyingTypeParameter = asTypeParameterOrArrayThereof(underlyingType)) {
        null -> substitutor.safeSubstitute(underlyingType)
        else -> substituteUpperBound(
            underlyingType,
            substitutor.safeSubstitute(underlyingTypeParameter.getRepresentativeUpperBound())
        )
    }
}

private fun TypeSystemCommonBackendContext.asTypeParameter(type: KotlinTypeMarker): TypeParameterMarker? =
    type.typeConstructor().getTypeParameterClassifier()

private fun TypeSystemCommonBackendContext.asTypeParameterOrArrayThereof(type: KotlinTypeMarker): TypeParameterMarker? {
    val typeParameter = asTypeParameter(type)
    return when {
        typeParameter != null -> typeParameter
        !type.isArrayOrNullableArray() -> null
        else -> type.getArguments().single().getType()?.let { asTypeParameterOrArrayThereof(it) }
    }
}

private fun TypeSystemCommonBackendContext.substituteUpperBound(type: KotlinTypeMarker, upperBound: KotlinTypeMarker): KotlinTypeMarker =
    when {
        asTypeParameter(type) != null -> if (type.isNullableType()) upperBound.makeNullable() else upperBound
        else -> {
            val elementArgument = type.getArguments().single()
            val elementTypeBounded = when (elementArgument.getVariance()) {
                TypeVariance.IN -> nullableAnyType()
                else -> substituteUpperBound(elementArgument.getType()!!, upperBound)
            }
            val arrayType = arrayType(elementTypeBounded)
            if (type.isNullableType()) arrayType.makeNullable() else arrayType
        }
    }
