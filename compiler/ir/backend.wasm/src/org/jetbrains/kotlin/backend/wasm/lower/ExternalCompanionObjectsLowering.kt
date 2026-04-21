/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isTmpForInline
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.IR_TEMPORARY_VARIABLE
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ExternalCompanionObjectsLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(ExternalCompanionObjectsLoweringTransformer())
    }
}

/**
 * Lightweight lowering for eliminating call to companion object getters with
 * unused result, when companion object is declared withing external interface.
 * Enables work with IDLs.
 */
private class ExternalCompanionObjectsLoweringTransformer : IrElementTransformerVoid() {

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
                if (expression.symbol == receiverVar) {
                    receiverUsed = true
                }
            }
        })

        // Null out initializers of tmp vars, if companion object instance isn't required
        if (!receiverUsed) {
            val excludedVars = listOf(receiverVar, helperVarForReceiver)

            inlinedBlock.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitBlock(expression: IrBlock): IrExpression {
                    expression.transformChildrenVoid(this)
                    expression.statements.removeAll { it in excludedVars }
                    return expression
                }

                override fun visitComposite(expression: IrComposite): IrExpression {
                    expression.transformChildrenVoid(this)
                    expression.statements.removeAll { it in excludedVars }
                    return expression
                }

                override fun visitBlockBody(body: IrBlockBody): IrBody {
                    body.transformChildrenVoid(this)
                    body.statements.removeAll { it in excludedVars }
                    return body
                }
            })
        }

        expression.transformChildrenVoid(this)

        return expression
    }

    private fun isExternalInterfaceCompanion(klass: IrClass): Boolean {
        if (!klass.isCompanion || !klass.isEffectivelyExternal()) return false
        val parent = klass.parent as? IrClass ?: return false
        return parent.isInterface
    }
}


