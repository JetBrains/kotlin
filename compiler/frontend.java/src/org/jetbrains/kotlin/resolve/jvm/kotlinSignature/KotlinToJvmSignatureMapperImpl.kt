/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.kotlinSignature

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.resolve.jvm.KotlinToJvmSignatureMapper
import org.jetbrains.kotlin.resolve.jvm.KotlinToJvmSignatureMapper.MethodSignature

@K1Deprecation
class KotlinToJvmSignatureMapperImpl : KotlinToJvmSignatureMapper {
    override fun mapToJvmMethodSignature(function: FunctionDescriptor): MethodSignature =
        MethodSignatureImpl(function.computeJvmDescriptor())

    override fun erasedSignaturesEqualIgnoringReturnTypes(subFunction: MethodSignature, superFunction: MethodSignature): Boolean =
        subFunction.parametersDescriptor() == superFunction.parametersDescriptor()

    private fun MethodSignature.parametersDescriptor(): String = (this as MethodSignatureImpl).desc.let {
        it.substring(it.lastIndexOf("(") + 1, it.lastIndexOf(")"))
    }

    private class MethodSignatureImpl(val desc: String) : MethodSignature
}
