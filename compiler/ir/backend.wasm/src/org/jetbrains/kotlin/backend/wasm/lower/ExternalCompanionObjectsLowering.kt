/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isTmpForInline
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.IR_TEMPORARY_VARIABLE
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ExternalCompanionObjectsLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(ExternalCompanionObjectsLoweringVisitor())
    }
}

/**
 * Lightweight lowering for eliminating call to companion object getters with
 * unused result, when companion object is declared withing external interface.
 * Enables work with IDLs.
 */
private class ExternalCompanionObjectsLoweringVisitor : IrElementTransformerVoid() {

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression.origin != IrStatementOrigin.INLINE_ARGS_CONTAINER) {
            // block is not inlined
            expression.transformChildrenVoid(this)
            return expression
        }

        val helperVarForReceiver = expression.statements[0] as? IrVariable ?: return expression
        // not an actual receiver
        if (helperVarForReceiver.origin != IR_TEMPORARY_VARIABLE && !helperVarForReceiver.isTmpForInline) return expression

        val init = helperVarForReceiver.initializer as? IrGetObjectValue ?: return expression
        // not a getter of companion object in external interface
        if (!isExternalInterfaceCompanion(init.symbol.owner)) return expression

        // extract actual inlined block according to the structure of inlining
        val inlinedBlock = expression.statements
            .filterIsInstance<IrReturnableBlock>()
            .firstOrNull()
            ?.statements
            ?.filterIsInstance<IrInlinedFunctionBlock>()
            ?.firstOrNull() ?: return expression

        val receiverVar = inlinedBlock.statements
            .filterIsInstance<IrVariable>()
            .find {
                it.isTmpForInline && (it.initializer as? IrGetValue)?.symbol == helperVarForReceiver.symbol
            } ?: return expression // return if receiver not found

        var receiverUsed = false
        inlinedBlock.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            override fun visitGetValue(expression: IrGetValue) {
                if (expression.symbol == receiverVar.symbol) {
                    receiverUsed = true
                }
            }
        })

        // remove temporary companion object instance vars, if companion object instance is dropped
        if (!receiverUsed) {
            val excludedVars = setOf(receiverVar, helperVarForReceiver)

            expression.eliminateTemporaryCompanionObjectInstanceVars(excludedVars)
        }

        expression.transformChildrenVoid(this)

        return expression
    }

    private fun IrBlock.eliminateTemporaryCompanionObjectInstanceVars(excludedVars: Set<IrVariable>) {
        acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)
            override fun visitContainerExpression(expression: IrContainerExpression) {
                expression.statements -= excludedVars
                expression.acceptChildrenVoid(this)
            }

            override fun visitBlockBody(body: IrBlockBody) {
                body.statements -= excludedVars
                body.acceptChildrenVoid(this)
            }
        })
    }

    private fun isExternalInterfaceCompanion(klass: IrClass): Boolean {
        if (!klass.isCompanion || !klass.isEffectivelyExternal()) return false
        val parent = klass.parent as? IrClass ?: return false
        return parent.isInterface
    }
}
