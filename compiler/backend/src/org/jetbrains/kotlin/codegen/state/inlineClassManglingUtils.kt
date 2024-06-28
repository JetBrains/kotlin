/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.codegen.coroutines.unwrapInitialDescriptorForSuspendFunction
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.isValueClass
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForParameterTypes
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForReturnType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import java.security.MessageDigest
import java.util.*

const val NOT_INLINE_CLASS_PARAMETER_PLACEHOLDER = "_"

class InfoForMangling(
    val fqName: FqNameUnsafe,
    val isValue: Boolean,
    val isNullable: Boolean
)

fun collectFunctionSignatureForManglingSuffix(
    useOldManglingRules: Boolean,
    requiresFunctionNameManglingForParameterTypes: Boolean,
    fqNamesForMangling: List<InfoForMangling?>,
    returnTypeInfo: InfoForMangling?,
): String? {
    fun getSignatureElementForMangling(info: InfoForMangling?): String = buildString {
        if (info == null) return ""
        if (useOldManglingRules || info.isValue) {
            append('L')
            append(info.fqName)
            if (info.isNullable) append('?')
            append(';')
        } else {
            append(NOT_INLINE_CLASS_PARAMETER_PLACEHOLDER)
        }
    }

    fun collectSignatureForMangling(): String =
        fqNamesForMangling.joinToString(separator = if (useOldManglingRules) ", " else "") {
            getSignatureElementForMangling(it)
        }

    if (useOldManglingRules) {
        if (requiresFunctionNameManglingForParameterTypes) {
            return collectSignatureForMangling()
        }

        // If a class member function returns inline class value, mangle its name.
        // NB here function can be a suspend function JVM view with return type replaced with 'Any',
        // should unwrap it and take original return type instead.
        if (returnTypeInfo != null) {
            return ":" + getSignatureElementForMangling(returnTypeInfo)
        }
    } else {
        // If a function accepts inline class parameters, mangle its name.
        if (requiresFunctionNameManglingForParameterTypes || returnTypeInfo != null) {
            // If a class member function returns inline class value, mangle its name.
            // NB here function can be a suspend function JVM view with return type replaced with 'Any',
            // should unwrap it and take original return type instead.
            val signature = collectSignatureForMangling() +
                    if (returnTypeInfo != null)
                        ":" + getSignatureElementForMangling(returnTypeInfo)
                    else ""
            return signature
        }
    }
    return null
}

fun getManglingSuffixBasedOnKotlinSignature(
    descriptor: CallableMemberDescriptor,
    shouldMangleByReturnType: Boolean,
    useOldManglingRules: Boolean
): String? {
    if (descriptor !is FunctionDescriptor) return null
    if (descriptor is ConstructorDescriptor) return null
    if (descriptor is JavaMethodDescriptor) return null
    if (InlineClassDescriptorResolver.isSynthesizedBoxOrUnboxMethod(descriptor)) return null

    // Don't mangle functions with '@JvmName' annotation.
    // Some stdlib functions ('Result.success', 'Result.failure') are annotated with '@JvmName' as a workaround for forward compatibility.
    if (DescriptorUtils.hasJvmNameAnnotation(descriptor)) return null

    val unwrappedDescriptor = descriptor.unwrapInitialDescriptorForSuspendFunction()

    val resultNew = collectFunctionSignatureForManglingSuffix(
        useOldManglingRules = useOldManglingRules,
        requiresFunctionNameManglingForParameterTypes = requiresFunctionNameManglingForParameterTypes(descriptor),
        fqNamesForMangling =
        (listOfNotNull(descriptor.extensionReceiverParameter?.type) + descriptor.valueParameters.map { it.type })
            .map { getInfoForMangling(it) },
        returnTypeInfo =
        if (shouldMangleByReturnType && requiresFunctionNameManglingForReturnType(unwrappedDescriptor))
            getInfoForMangling(unwrappedDescriptor.returnType!!)
        else null
    )

    return resultNew?.let { "-" + md5base64(it) }
}

private fun getInfoForMangling(type: KotlinType): InfoForMangling? {
    val descriptor = type.constructor.declarationDescriptor ?: return null
    return when (descriptor) {
        is ClassDescriptor -> InfoForMangling(descriptor.fqNameUnsafe, descriptor.isValueClass(), type.isMarkedNullable)

        is TypeParameterDescriptor -> {
            getInfoForMangling(descriptor.representativeUpperBound)
        }

        else -> null
    }
}

fun md5base64(signatureForMangling: String): String {
    val d = MessageDigest.getInstance("MD5").digest(signatureForMangling.toByteArray()).copyOfRange(0, 5)
    // base64 URL encoder without padding uses exactly the characters allowed in both JVM bytecode and Dalvik bytecode names
    return Base64.getUrlEncoder().withoutPadding().encodeToString(d)
}
