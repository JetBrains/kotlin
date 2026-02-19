/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.signature

import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.resolve.jvm.KotlinToJvmSignatureMapper
import org.jetbrains.kotlin.resolve.jvm.KotlinToJvmSignatureMapper.MethodSignature
import org.jetbrains.org.objectweb.asm.commons.Method

class KotlinToJvmSignatureMapperImpl : KotlinToJvmSignatureMapper {
    private val typeMapper = KotlinTypeMapper(
        JvmProtoBufUtil.DEFAULT_MODULE_NAME,
        LanguageVersionSettingsImpl.DEFAULT,
        useOldInlineClassesManglingScheme = false
    )

    override fun mapToJvmMethodSignature(function: FunctionDescriptor): MethodSignature =
        MethodSignatureImpl(typeMapper.mapAsmMethod(function))

    override fun erasedSignaturesEqualIgnoringReturnTypes(subFunction: MethodSignature, superFunction: MethodSignature): Boolean =
        subFunction.parametersDescriptor() == superFunction.parametersDescriptor()

    private fun MethodSignature.parametersDescriptor(): String = (this as MethodSignatureImpl).method.let { method ->
        method.descriptor.substring(1, method.descriptor.lastIndexOf(")"))
    }

    private class MethodSignatureImpl(val method: Method) : MethodSignature
}
