/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.kotlin.resolve.unsubstitutedUnderlyingType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

internal fun computeUnderlyingType(inlineClassType: KotlinType): KotlinType? {
    if (!shouldUseUnderlyingType(inlineClassType)) return null

    val descriptor = inlineClassType.unsubstitutedUnderlyingType()?.constructor?.declarationDescriptor ?: return null
    return if (descriptor is TypeParameterDescriptor)
        descriptor.representativeUpperBound
    else
        inlineClassType.substitutedUnderlyingType()
}

internal fun computeExpandedTypeForInlineClass(inlineClassType: KotlinType): KotlinType? =
    computeExpandedTypeInner(inlineClassType, hashSetOf())

private fun computeExpandedTypeInner(kotlinType: KotlinType, visitedClassifiers: HashSet<ClassifierDescriptor>): KotlinType? {
    val classifier = kotlinType.constructor.declarationDescriptor
        ?: throw AssertionError("Type with a declaration expected: $kotlinType")
    if (!visitedClassifiers.add(classifier)) return null

    return when {
        classifier is TypeParameterDescriptor ->
            computeExpandedTypeInner(classifier.representativeUpperBound, visitedClassifiers)
                ?.let { expandedUpperBound ->
                    if (expandedUpperBound.isNullable() || !kotlinType.isMarkedNullable)
                        expandedUpperBound
                    else
                        expandedUpperBound.makeNullable()
                }

        classifier is ClassDescriptor && classifier.isInline -> {
            // kotlinType is the boxed inline class type

            val underlyingType = kotlinType.substitutedUnderlyingType() ?: return null
            val expandedUnderlyingType = computeExpandedTypeInner(underlyingType, visitedClassifiers) ?: return null
            when {
                !kotlinType.isNullable() -> expandedUnderlyingType

                // Here inline class type is nullable. Apply nullability to the expandedUnderlyingType.

                // Nullable types become inline class boxes
                expandedUnderlyingType.isNullable() -> kotlinType

                // Primitives become inline class boxes
                KotlinBuiltIns.isPrimitiveType(expandedUnderlyingType) -> kotlinType

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
