/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir.transformations

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.detach
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.replaceWith
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.psi2ir.containsNull
import org.jetbrains.kotlin.psi2ir.defaultLoad
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.constNull
import org.jetbrains.kotlin.psi2ir.generators.equalsNull
import org.jetbrains.kotlin.psi2ir.intermediate.OnceExpressionValue

fun inlineSafeCallChains(context: GeneratorContext, element: IrElement) {
    element.accept(InlineSafeCallChains(context), null)
}

class InlineSafeCallChains(val context: GeneratorContext) : IrElementVisitor<Unit, Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?) {
        element.acceptChildren(this, data)
    }

    override fun visitBlock(expression: IrBlock, data: Nothing?) {
        expression.acceptChildren(this, data)

        if (expression.operator == IrOperator.SAFE_CALL) {
            val safeCall = getSafeCallInfo(expression) ?: return
            val innerSafeCall = (safeCall.receiverValue as? IrBlock)?.let { getSafeCallInfo(it) } ?: return
            rewriteSafeCallChain(safeCall, innerSafeCall)
        }
    }

    private fun rewriteSafeCallChain(outer: SafeCallInfo, inner: SafeCallInfo) {
        val innerNestedCallReturnType = inner.nestedCall.type ?: return
        if (innerNestedCallReturnType.containsNull()) return

        outer.root.replaceWith {
            val newBlock = IrBlockImpl(it.startOffset, it.endOffset, it.type, it.hasResult, IrOperator.SAFE_CALL)
            newBlock.addStatement(inner.receiverVariable.detach())
            outer.nestedCall.acceptChildren(
                    ReplaceTemporaryVariable(outer.receiverVariable, OnceExpressionValue(inner.nestedCall.detach())),
                    null)
            newBlock.addStatement(IrIfThenElseImpl(
                    it.startOffset, it.endOffset, it.type,
                    context.equalsNull(it.startOffset, it.endOffset, inner.receiverVariable.defaultLoad()),
                    context.constNull(it.startOffset, it.endOffset),
                    outer.nestedCall.detach(),
                    IrOperator.SAFE_CALL))
            newBlock
        }
    }

    private class SafeCallInfo(val root: IrBlock, val receiverVariable: IrVariable, val nestedCall: IrExpression) {
        val receiverValue = receiverVariable.initializer
    }

    private fun getSafeCallInfo(block: IrBlock): SafeCallInfo? {
        if (block.operator != IrOperator.SAFE_CALL) return null
        val receiverVariable = block.statements[0] as? IrVariable ?: return null
        if (receiverVariable.initializer == null) return null
        val nestedCall = (block.statements[1] as? IrWhen)?.elseBranch ?: return null
        return SafeCallInfo(block, receiverVariable, nestedCall)
    }
}

