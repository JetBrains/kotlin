/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.collectRealOverrides
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

/**
 * Computes the JVM signature of a given IrFunction. The function is passed as an IrFunctionReference
 * to the single argument of the intrinsic.
 */
object SignatureString : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        val argument = generateSequence(expression.getValueArgument(0) as IrStatement) { (it as? IrBlock)?.statements?.lastOrNull() }
            .filterIsInstance<IrFunctionReference>().single()
        val function = argument.symbol.owner
        generateSignatureString(codegen.mv, function, codegen.classCodegen)
        return MaterialValue(codegen, AsmTypes.JAVA_STRING_TYPE, codegen.context.irBuiltIns.stringType)
    }

    internal fun generateSignatureString(v: InstructionAdapter, function: IrFunction, codegen: ClassCodegen) {
        var resolved = when (function) {
            is IrSimpleFunction -> function.collectRealOverrides().first()
            is IrConstructor -> function
        }
        if (resolved.isSuspend) {
            resolved = codegen.context.suspendFunctionOriginalToView[resolved] ?: resolved
        }
        val method = codegen.methodSignatureMapper.mapAsmMethod(resolved)
        val descriptor = method.name + method.descriptor
        v.aconst(descriptor)
    }
}
