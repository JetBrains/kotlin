/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.FunctionDescriptor

interface KotlinToJvmSignatureMapper {
    fun mapToJvmMethodSignature(function: FunctionDescriptor): MethodSignature

    fun erasedSignaturesEqualIgnoringReturnTypes(subFunction: MethodSignature, superFunction: MethodSignature): Boolean

    interface MethodSignature
}
