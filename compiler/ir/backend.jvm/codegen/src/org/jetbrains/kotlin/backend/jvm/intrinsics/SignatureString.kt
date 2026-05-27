/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrRawFunctionReference
import org.jetbrains.kotlin.resolve.jvm.AsmTypes

/**
 * Computes the JVM signature of a given IrFunction. The function is passed as an IrRawFunctionReference
 * to the single argument of the intrinsic.
 */
object SignatureString : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        val argument = generateSequence(expression.arguments[0] as IrStatement) { (it as? IrBlock)?.statements?.lastOrNull() }
            .filterIsInstance<IrRawFunctionReference>().single()
        val function = argument.symbol.owner
        codegen.mv.aconst(codegen.classCodegen.methodSignatureMapper.generateSignatureString(function))
        return MaterialValue(codegen, AsmTypes.JAVA_STRING_TYPE, codegen.context.irBuiltIns.stringType)
    }
}
