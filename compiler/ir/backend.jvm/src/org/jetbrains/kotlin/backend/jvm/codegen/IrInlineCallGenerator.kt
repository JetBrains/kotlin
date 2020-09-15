/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.lower.suspendFunctionOriginal
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.render

interface IrInlineCallGenerator : IrCallGenerator {
    override fun genCall(
        callableMethod: IrCallableMethod,
        codegen: ExpressionCodegen,
        expression: IrFunctionAccessExpression,
        isInsideIfCondition: Boolean,
    ) {
        val element = codegen.context.psiSourceManager.findPsiElement(expression, codegen.irFunction)
            ?: codegen.context.psiSourceManager.findPsiElement(codegen.irFunction)
        if (!codegen.state.globalInlineContext.enterIntoInlining(
                expression.symbol.owner.suspendFunctionOriginal().toIrBasedDescriptor(), element)
        ) {
            val message = "Call is a part of inline call cycle: ${expression.render()}"
            AsmUtil.genThrow(codegen.visitor, "java/lang/UnsupportedOperationException", message)
            return
        }
        try {
            genInlineCall(callableMethod, codegen, expression, isInsideIfCondition)
        } finally {
            codegen.state.globalInlineContext.exitFromInlining()
        }
    }

    fun genInlineCall(
        callableMethod: IrCallableMethod,
        codegen: ExpressionCodegen,
        expression: IrFunctionAccessExpression,
        isInsideIfCondition: Boolean,
    )
}
