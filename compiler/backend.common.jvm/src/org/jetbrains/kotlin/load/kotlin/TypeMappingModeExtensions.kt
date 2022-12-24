/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

fun TypeSystemCommonBackendContext.getOptimalModeForValueParameter(
    type: KotlinTypeMarker
): TypeMappingMode = getOptimalModeForSignaturePart(type, canBeUsedInSupertypePosition = true)

fun TypeSystemCommonBackendContext.getOptimalModeForReturnType(
    type: KotlinTypeMarker,
    isAnnotationMethod: Boolean
): TypeMappingMode {
    return if (isAnnotationMethod)
        TypeMappingMode.VALUE_FOR_ANNOTATION
    else
        getOptimalModeForSignaturePart(type, canBeUsedInSupertypePosition = false)
}

@OptIn(TypeMappingModeInternals::class)
private fun TypeSystemCommonBackendContext.getOptimalModeForSignaturePart(
    type: KotlinTypeMarker,
    canBeUsedInSupertypePosition: Boolean
): TypeMappingMode {
    if (type.argumentsCount() == 0) return TypeMappingMode.DEFAULT

    val isInlineClassType = type.typeConstructor().isInlineClass()
    if (isInlineClassType && shouldUseUnderlyingType(type)) {
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
        needInlineClassWrapping = !isInlineClassType
    )
}
