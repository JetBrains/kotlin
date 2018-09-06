/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.getRepresentativeUpperBound
import org.jetbrains.kotlin.resolve.DescriptorUtils.RESULT_FQ_NAME
import org.jetbrains.kotlin.resolve.DescriptorUtils.SUCCESS_OR_FAILURE_FQ_NAME
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.types.KotlinType
import java.security.MessageDigest
import java.util.*

fun getInlineClassSignatureManglingSuffix(descriptor: CallableMemberDescriptor): String? {
    if (descriptor !is FunctionDescriptor) return null
    if (descriptor is ConstructorDescriptor) return null
    if (InlineClassDescriptorResolver.isSynthesizedBoxOrUnboxMethod(descriptor)) return null

    val actualValueParameterTypes = listOfNotNull(descriptor.extensionReceiverParameter?.type) + descriptor.valueParameters.map { it.type }

    return getInlineClassSignatureManglingSuffix(actualValueParameterTypes)
}

fun getInlineClassSignatureManglingSuffix(valueParameterTypes: List<KotlinType>) =
    if (valueParameterTypes.none { it.requiresFunctionNameMangling() })
        null
    else
        "-" + md5base64(collectSignatureForMangling(valueParameterTypes))

private fun KotlinType.requiresFunctionNameMangling() =
    isInlineClassThatRequiresMangling() || isTypeParameterWithUpperBoundThatRequiresMangling()

private fun KotlinType.isInlineClassThatRequiresMangling() =
    isInlineClassType() && !isDontMangleClass(this.constructor.declarationDescriptor as ClassDescriptor)

private fun isDontMangleClass(classDescriptor: ClassDescriptor) =
    classDescriptor.fqNameSafe == SUCCESS_OR_FAILURE_FQ_NAME || classDescriptor.fqNameSafe == RESULT_FQ_NAME

private fun KotlinType.isTypeParameterWithUpperBoundThatRequiresMangling(): Boolean {
    val descriptor = constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    return getRepresentativeUpperBound(descriptor).requiresFunctionNameMangling()
}

private fun collectSignatureForMangling(types: List<KotlinType>) =
    types.joinToString { getSignatureElementForMangling(it) }

private fun getSignatureElementForMangling(type: KotlinType): String = buildString {
    val descriptor = type.constructor.declarationDescriptor ?: return ""
    when (descriptor) {
        is ClassDescriptor -> {
            append('L')
            append(descriptor.fqNameUnsafe)
            if (type.isMarkedNullable) append('?')
            append(';')
        }

        is TypeParameterDescriptor -> {
            append(getSignatureElementForMangling(getRepresentativeUpperBound(descriptor)))
        }
    }
}

private fun md5base64(signatureForMangling: String): String {
    val d = MessageDigest.getInstance("MD5").digest(signatureForMangling.toByteArray()).copyOfRange(0, 5)
    // base64 URL encoder without padding uses exactly the characters allowed in both JVM bytecode and Dalvik bytecode names
    return Base64.getUrlEncoder().withoutPadding().encodeToString(d)
}
