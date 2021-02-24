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
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

internal fun computeUnderlyingType(inlineClassType: KotlinType): KotlinType? {
    if (!shouldUseUnderlyingType(inlineClassType)) return null

    val descriptor = inlineClassType.unsubstitutedUnderlyingType()?.constructor?.declarationDescriptor ?: return null
    return if (descriptor is TypeParameterDescriptor)
        descriptor.representativeUpperBound
    else
        inlineClassType.substitutedUnderlyingType()
}

internal fun shouldUseUnderlyingType(inlineClassType: KotlinType): Boolean {
    val underlyingType = inlineClassType.unsubstitutedUnderlyingType() ?: return false

    return !inlineClassType.isMarkedNullable ||
            !TypeUtils.isNullableType(underlyingType) && !KotlinBuiltIns.isPrimitiveType(underlyingType)
}
