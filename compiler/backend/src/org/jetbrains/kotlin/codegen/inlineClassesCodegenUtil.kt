/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.underlyingRepresentation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

fun KotlinType.isInlineClassWithUnderlyingTypeAnyOrAnyN(): Boolean {
    if (!isInlineClassType()) return false
    val classDescriptor = constructor.declarationDescriptor as? ClassDescriptor ?: return false
    return classDescriptor.underlyingRepresentation()?.type?.isAnyOrNullableAny() == true
}

fun CallableDescriptor.isGenericParameter(): Boolean {
    if (this !is ValueParameterDescriptor) return false
    if (containingDeclaration is AnonymousFunctionDescriptor) return true
    val index = containingDeclaration.valueParameters.indexOf(this)
    return containingDeclaration.overriddenDescriptors.any { it.original.valueParameters[index].type.isTypeParameter() }
}