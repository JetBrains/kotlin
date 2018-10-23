/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.getRepresentativeUpperBound
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.types.KotlinType
import java.security.MessageDigest
import java.util.*

fun shouldHideConstructorDueToInlineClassTypeValueParameters(descriptor: CallableMemberDescriptor): Boolean {
    val constructorDescriptor = descriptor as? ClassConstructorDescriptor ?: return false
    if (Visibilities.isPrivate(constructorDescriptor.visibility)) return false
    if (constructorDescriptor.constructedClass.isInline) return false
    if (DescriptorUtils.isSealedClass(constructorDescriptor.constructedClass)) return false

    // TODO inner class in inline class

    return constructorDescriptor.valueParameters.any { it.type.requiresFunctionNameMangling() }
}

fun requiresFunctionNameMangling(valueParameterTypes: List<KotlinType>): Boolean {
    return valueParameterTypes.any { it.requiresFunctionNameMangling() }
}

fun DeclarationDescriptor.isInlineClassThatRequiresMangling(): Boolean =
    isInlineClass() && !isDontMangleClass(this as ClassDescriptor)

fun KotlinType.isInlineClassThatRequiresMangling() =
    constructor.declarationDescriptor?.isInlineClassThatRequiresMangling() == true

private fun KotlinType.requiresFunctionNameMangling() =
    isInlineClassThatRequiresMangling() || isTypeParameterWithUpperBoundThatRequiresMangling()

private fun isDontMangleClass(classDescriptor: ClassDescriptor) =
    classDescriptor.fqNameSafe == DescriptorUtils.RESULT_FQ_NAME

private fun KotlinType.isTypeParameterWithUpperBoundThatRequiresMangling(): Boolean {
    val descriptor = constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    return getRepresentativeUpperBound(descriptor).requiresFunctionNameMangling()
}
