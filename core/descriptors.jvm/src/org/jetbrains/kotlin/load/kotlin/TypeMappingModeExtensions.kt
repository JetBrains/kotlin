/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.types.KotlinType

fun TypeMappingMode.Companion.getOptimalModeForValueParameter(
    type: KotlinType
): TypeMappingMode = getOptimalModeForSignaturePart(type, canBeUsedInSupertypePosition = true)

fun TypeMappingMode.Companion.getOptimalModeForReturnType(
    type: KotlinType,
    isAnnotationMethod: Boolean
): TypeMappingMode {
    return if (isAnnotationMethod)
        VALUE_FOR_ANNOTATION
    else
        getOptimalModeForSignaturePart(type, canBeUsedInSupertypePosition = false)
}

@OptIn(TypeMappingModeInternals::class)
private fun getOptimalModeForSignaturePart(type: KotlinType, canBeUsedInSupertypePosition: Boolean): TypeMappingMode {
    if (type.arguments.isEmpty()) return TypeMappingMode.DEFAULT

    if (type.isInlineClassType() && shouldUseUnderlyingType(type)) {
        val underlyingType = computeUnderlyingType(type)
        if (underlyingType != null) {
            return getOptimalModeForSignaturePart(underlyingType, canBeUsedInSupertypePosition).dontWrapInlineClassesMode()
        }
    }

    val contravariantArgumentMode =
        if (!canBeUsedInSupertypePosition)
            TypeMappingMode(skipDeclarationSiteWildcards = false, skipDeclarationSiteWildcardsIfPossible = true)
        else
            null

    val invariantArgumentMode =
        if (canBeUsedInSupertypePosition)
            getOptimalModeForSignaturePart(type, canBeUsedInSupertypePosition = false)
        else
            null

    return TypeMappingMode(
        skipDeclarationSiteWildcards = !canBeUsedInSupertypePosition,
        skipDeclarationSiteWildcardsIfPossible = true,
        genericContravariantArgumentMode = contravariantArgumentMode,
        genericInvariantArgumentMode = invariantArgumentMode,
        needInlineClassWrapping = !type.isInlineClassType()
    )
}
