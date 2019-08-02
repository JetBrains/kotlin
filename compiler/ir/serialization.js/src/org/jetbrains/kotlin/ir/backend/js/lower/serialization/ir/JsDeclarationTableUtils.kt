/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.psi2ir.findFirstFunction

internal const val PUBLIC_LOCAL_UNIQ_ID_EDGE = 0x7FFF_FFFF_FFFF_FFFFL + 1L
internal const val BUILT_IN_FUNCTION_CLASS_COUNT = 4
internal const val BUILT_IN_FUNCTION_ARITY_COUNT = 256
internal const val BUILT_IN_UNIQ_ID_GAP = 2 * BUILT_IN_FUNCTION_ARITY_COUNT * BUILT_IN_FUNCTION_CLASS_COUNT
internal const val BUILT_IN_UNIQ_ID_CLASS_OFFSET = BUILT_IN_FUNCTION_CLASS_COUNT * BUILT_IN_FUNCTION_ARITY_COUNT

private fun builtInOffset(function: IrSimpleFunction): Long {
    val isK = function.parentAsClass.name.asString().startsWith("K")
    return when {
        isK && function.isSuspend -> 3
        isK -> 2
        function.isSuspend -> 1
        else -> 0
    }
}

internal fun builtInFunctionId(value: IrDeclaration): Long = when (value) {
    is IrSimpleFunction -> {
        value.run { valueParameters.size + builtInOffset(value) * BUILT_IN_FUNCTION_ARITY_COUNT }.toLong()
    }
    is IrClass -> {
        BUILT_IN_UNIQ_ID_CLASS_OFFSET + builtInFunctionId(value.declarations.first { it.nameForIrSerialization.asString() == "invoke" })
    }
    else -> error("Only class or function is expected")
}

private fun builtInOffset(function: FunctionInvokeDescriptor): Long {
    val isK = function.containingDeclaration.name.asString().startsWith("K")
    return when {
        isK && function.isSuspend -> 3
        isK -> 2
        function.isSuspend -> 1
        else -> 0
    }
}

internal fun builtInFunctionId(value: DeclarationDescriptor): Long = when (value) {
    is FunctionInvokeDescriptor -> {
        value.run { valueParameters.size + builtInOffset(value) * BUILT_IN_FUNCTION_ARITY_COUNT }.toLong()
    }
    is ClassDescriptor -> {
        BUILT_IN_UNIQ_ID_CLASS_OFFSET + builtInFunctionId(value.findFirstFunction("invoke") { true })
    }
    else -> error("Only class or function is expected")
}
