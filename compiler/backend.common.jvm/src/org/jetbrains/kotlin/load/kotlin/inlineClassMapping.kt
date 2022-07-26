/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

internal fun TypeSystemCommonBackendContext.computeUnderlyingType(inlineClassType: KotlinTypeMarker): KotlinTypeMarker? {
    if (!shouldUseUnderlyingType(inlineClassType)) return null

    val underlyingType = inlineClassType.getUnsubstitutedUnderlyingType() ?: return null
    return underlyingType.typeConstructor().getTypeParameterClassifier()?.getRepresentativeUpperBound()
        ?: inlineClassType.getSubstitutedUnderlyingType()
}

internal fun TypeSystemCommonBackendContext.shouldUseUnderlyingType(inlineClassType: KotlinTypeMarker): Boolean {
    val underlyingType = inlineClassType.getUnsubstitutedUnderlyingType() ?: return false

    return !inlineClassType.isMarkedNullable() ||
            !underlyingType.isNullableType() && !(underlyingType is SimpleTypeMarker && underlyingType.isPrimitiveType())
}
