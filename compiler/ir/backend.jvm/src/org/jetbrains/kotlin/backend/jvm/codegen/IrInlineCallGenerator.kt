/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.psi.PsiSourceManager
import org.jetbrains.kotlin.backend.jvm.lower.suspendFunctionOriginal
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression

interface IrInlineCallGenerator : IrCallGenerator {
    override fun genCall(
        callableMethod: IrCallableMethod,
        codegen: ExpressionCodegen,
        expression: IrFunctionAccessExpression,
        isInsideIfCondition: Boolean,
    ) {
        val element = PsiSourceManager.findPsiElement(expression, codegen.irFunction)
            ?: PsiSourceManager.findPsiElement(codegen.irFunction)
        val descriptor = expression.symbol.owner.suspendFunctionOriginal().toIrBasedDescriptor()
        if (!codegen.state.globalInlineContext.enterIntoInlining(descriptor, element)) {
            genCycleStub(expression.psiElement?.text ?: "<no source>", codegen)
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

    fun genCycleStub(text: String, codegen: ExpressionCodegen) {
        AsmUtil.genThrow(codegen.visitor, "java/lang/UnsupportedOperationException", "Call is a part of inline call cycle: $text")
    }
}
