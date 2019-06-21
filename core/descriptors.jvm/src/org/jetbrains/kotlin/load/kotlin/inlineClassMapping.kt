/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.kotlin.resolve.unsubstitutedUnderlyingType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

internal fun computeUnderlyingType(inlineClassType: KotlinType): KotlinType? {
    if (!shouldUseUnderlyingType(inlineClassType)) return null

    val descriptor = inlineClassType.unsubstitutedUnderlyingType()?.constructor?.declarationDescriptor ?: return null
    return if (descriptor is TypeParameterDescriptor)
        descriptor.representativeUpperBound
    else
        inlineClassType.substitutedUnderlyingType()
}

fun TypeSystemCommonBackendContext.computeExpandedTypeForInlineClass(inlineClassType: KotlinTypeMarker): KotlinTypeMarker? =
    computeExpandedTypeInner(inlineClassType, hashSetOf())

private fun TypeSystemCommonBackendContext.computeExpandedTypeInner(
    kotlinType: KotlinTypeMarker, visitedClassifiers: HashSet<TypeConstructorMarker>
): KotlinTypeMarker? {
    val classifier = kotlinType.typeConstructor()
    if (!visitedClassifiers.add(classifier)) return null

    val typeParameter = classifier.getTypeParameterClassifier()

    return when {
        typeParameter != null ->
            computeExpandedTypeInner(typeParameter.getRepresentativeUpperBound(), visitedClassifiers)
                ?.let { expandedUpperBound ->
                    if (expandedUpperBound.isNullableType() || !kotlinType.isMarkedNullable())
                        expandedUpperBound
                    else
                        expandedUpperBound.makeNullable()
                }

        classifier.isInlineClass() -> {
            // kotlinType is the boxed inline class type

            val underlyingType = kotlinType.getSubstitutedUnderlyingType() ?: return null
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

internal fun shouldUseUnderlyingType(inlineClassType: KotlinType): Boolean {
    val underlyingType = inlineClassType.unsubstitutedUnderlyingType() ?: return false

    return !inlineClassType.isMarkedNullable ||
            !TypeUtils.isNullableType(underlyingType) && !KotlinBuiltIns.isPrimitiveType(underlyingType)
}
