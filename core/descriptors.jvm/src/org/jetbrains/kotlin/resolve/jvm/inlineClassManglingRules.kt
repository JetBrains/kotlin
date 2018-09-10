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
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.types.KotlinType
import java.security.MessageDigest
import java.util.*

fun shouldHideConstructorDueToInlineClassTypeValueParameters(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor !is ClassConstructorDescriptor) return false
    if (Visibilities.isPrivate(descriptor.visibility)) return false
    if (descriptor.constructedClass.isInline) return false

    // TODO inner class in inline class

    return descriptor.valueParameters.any { it.type.requiresFunctionNameMangling() }
}

fun requiresFunctionNameMangling(valueParameterTypes: List<KotlinType>): Boolean {
    return valueParameterTypes.any { it.requiresFunctionNameMangling() }
}

private fun KotlinType.requiresFunctionNameMangling() =
    isInlineClassThatRequiresMangling() || isTypeParameterWithUpperBoundThatRequiresMangling()

private fun KotlinType.isInlineClassThatRequiresMangling() =
    isInlineClassType() && !isDontMangleClass(this.constructor.declarationDescriptor as ClassDescriptor)

private fun isDontMangleClass(classDescriptor: ClassDescriptor) =
    classDescriptor.fqNameSafe == DescriptorUtils.RESULT_FQ_NAME

private fun KotlinType.isTypeParameterWithUpperBoundThatRequiresMangling(): Boolean {
    val descriptor = constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    return getRepresentativeUpperBound(descriptor).requiresFunctionNameMangling()
}
