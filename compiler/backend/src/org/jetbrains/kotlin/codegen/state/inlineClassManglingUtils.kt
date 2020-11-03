/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.coroutines.unwrapInitialDescriptorForSuspendFunction
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForParameterTypes
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForReturnType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberOrNullableType
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import java.security.MessageDigest
import java.util.*

fun getManglingSuffixBasedOnKotlinSignature(
    descriptor: CallableMemberDescriptor,
    shouldMangleByReturnType: Boolean
): String? {
    if (descriptor !is FunctionDescriptor) return null
    if (descriptor is ConstructorDescriptor) return null
    if (InlineClassDescriptorResolver.isSynthesizedBoxOrUnboxMethod(descriptor)) return null

    // Don't mangle functions with '@JvmName' annotation.
    // Some stdlib functions ('Result.success', 'Result.failure') are annotated with '@JvmName' as a workaround for forward compatibility.
    if (DescriptorUtils.hasJvmNameAnnotation(descriptor)) return null

    val unwrappedDescriptor = descriptor.unwrapInitialDescriptorForSuspendFunction()

    // If a function accepts inline class parameters, mangle its name.
    if (requiresFunctionNameManglingForParameterTypes(descriptor) ||
        (shouldMangleByReturnType && requiresFunctionNameManglingForReturnType(unwrappedDescriptor))
    ) {
        // If a class member function returns inline class value, mangle its name.
        // NB here function can be a suspend function JVM view with return type replaced with 'Any',
        // should unwrap it and take original return type instead.
        val signature = collectSignatureForMangling(descriptor) +
                if (shouldMangleByReturnType && requiresFunctionNameManglingForReturnType(unwrappedDescriptor))
                    ":" + getSignatureElementForMangling(unwrappedDescriptor.returnType!!)
                else ""
        return "-" + md5base64(signature)
    }
    return null
}

private fun collectSignatureForMangling(descriptor: CallableMemberDescriptor): String {
    val types = listOfNotNull(descriptor.extensionReceiverParameter?.type) + descriptor.valueParameters.map { it.type }
    return types.joinToString { getSignatureElementForMangling(it) }
}

private fun getSignatureElementForMangling(type: KotlinType): String = buildString {
    val descriptor = type.constructor.declarationDescriptor ?: return ""
    when (descriptor) {
        is ClassDescriptor -> if (descriptor.isInline) {
            append('L')
            append(descriptor.fqNameUnsafe)
            if (type.isMarkedNullable) append('?')
            append(';')
        } else {
            append('x')
        }

        is TypeParameterDescriptor -> {
            append(getSignatureElementForMangling(descriptor.representativeUpperBound))
        }
    }
}

fun md5base64(signatureForMangling: String): String {
    val d = MessageDigest.getInstance("MD5").digest(signatureForMangling.toByteArray()).copyOfRange(0, 5)
    // base64 URL encoder without padding uses exactly the characters allowed in both JVM bytecode and Dalvik bytecode names
    return Base64.getUrlEncoder().withoutPadding().encodeToString(d)
}
