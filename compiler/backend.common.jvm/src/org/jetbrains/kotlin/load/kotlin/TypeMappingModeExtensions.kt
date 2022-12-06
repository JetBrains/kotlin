/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

fun TypeSystemCommonBackendContext.getOptimalModeForValueParameter(
    type: KotlinTypeMarker,
    isForUast: Boolean = false,
): TypeMappingMode = getOptimalModeForSignaturePart(type, canBeUsedInSupertypePosition = true, isForUast)

fun TypeSystemCommonBackendContext.getOptimalModeForReturnType(
    type: KotlinTypeMarker,
    isAnnotationMethod: Boolean,
    isForUast: Boolean = false,
): TypeMappingMode {
    return if (isAnnotationMethod)
        TypeMappingMode.VALUE_FOR_ANNOTATION
    else
        getOptimalModeForSignaturePart(type, canBeUsedInSupertypePosition = false, isForUast)
}

@OptIn(TypeMappingModeInternals::class)
private fun TypeSystemCommonBackendContext.getOptimalModeForSignaturePart(
    type: KotlinTypeMarker,
    canBeUsedInSupertypePosition: Boolean,
    isForUast: Boolean = false,
): TypeMappingMode {
    if (type.argumentsCount() == 0) return TypeMappingMode.DEFAULT

    val isInlineClassType = type.typeConstructor().isInlineClass()
    if (isInlineClassType && shouldUseUnderlyingType(type)) {
        val underlyingType = computeUnderlyingType(type)
        if (underlyingType != null) {
            return getOptimalModeForSignaturePart(underlyingType, canBeUsedInSupertypePosition, isForUast).dontWrapInlineClassesMode()
        }
    }

    val contravariantArgumentMode =
        if (!canBeUsedInSupertypePosition)
            TypeMappingMode(skipDeclarationSiteWildcards = false, skipDeclarationSiteWildcardsIfPossible = true)
        else
            null

    val invariantArgumentMode =
        if (canBeUsedInSupertypePosition)
            getOptimalModeForSignaturePart(type, canBeUsedInSupertypePosition = false, isForUast)
        else
            null

    return TypeMappingMode(
        skipDeclarationSiteWildcards = !canBeUsedInSupertypePosition,
        skipDeclarationSiteWildcardsIfPossible = true,
        genericArgumentMode = if (isForUast) TypeMappingMode.GENERIC_ARGUMENT_UAST else null,
        genericContravariantArgumentMode = contravariantArgumentMode,
        genericInvariantArgumentMode = invariantArgumentMode,
        needInlineClassWrapping = !isInlineClassType,
        mapTypeAliases = isForUast
    )
}
