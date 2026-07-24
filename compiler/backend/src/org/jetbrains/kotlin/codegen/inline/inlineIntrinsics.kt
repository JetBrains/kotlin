/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForCoroutineContext
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.isTopLevelInPackage
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.org.objectweb.asm.Type

private fun IrFunction.isBuiltInSuspendCoroutineUninterceptedOrReturn(): Boolean =
    isTopLevelInPackage("suspendCoroutineUninterceptedOrReturn", StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME)

fun generateInlineIntrinsicForIr(function: IrFunction): SMAPAndMethodNode? =
    when {
        // TODO: implement these as codegen intrinsics (see IrIntrinsicMethods)
        function.isBuiltInCoroutineContext() ->
            createMethodNodeForCoroutineContext(function)
        function.isBuiltInSuspendCoroutineUninterceptedOrReturn() ->
            createMethodNodeForSuspendCoroutineUninterceptedOrReturn()
        else -> null
    }?.let { SMAPAndMethodNode(it, SMAP(listOf())) }

internal fun getSpecialEnumFunDescriptor(type: Type, isValueOf: Boolean): String =
    if (isValueOf) Type.getMethodDescriptor(type, JAVA_STRING_TYPE)
    else Type.getMethodDescriptor(AsmUtil.getArrayType(type))

internal fun IrFunction.isBuiltInCoroutineContext(): Boolean =
    this is IrSimpleFunction && correspondingPropertySymbol?.owner?.isTopLevelInPackage(
        "coroutineContext", StandardNames.COROUTINES_PACKAGE_FQ_NAME,
    ) == true
