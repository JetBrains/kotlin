/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.backend.common.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForCoroutineContext
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.isBuiltInCoroutineContext
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.org.objectweb.asm.Type

fun generateInlineIntrinsicForIr(descriptor: FunctionDescriptor): SMAPAndMethodNode? =
    when {
        // TODO: implement these as codegen intrinsics (see IrIntrinsicMethods)
        descriptor.isBuiltInCoroutineContext() ->
            createMethodNodeForCoroutineContext(descriptor)
        descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn() ->
            createMethodNodeForSuspendCoroutineUninterceptedOrReturn()
        else -> null
    }?.let { SMAPAndMethodNode(it, SMAP(listOf())) }

internal fun getSpecialEnumFunDescriptor(type: Type, isValueOf: Boolean): String =
    if (isValueOf) Type.getMethodDescriptor(type, JAVA_STRING_TYPE)
    else Type.getMethodDescriptor(AsmUtil.getArrayType(type))
