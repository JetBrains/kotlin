/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.visitors.*

class WasmEliminateUnusedLocalVariables(val context: WasmBackendContext): FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            override fun visitFunction(declaration: IrFunction) {
                declaration.acceptChildrenVoid(this)
                val body = declaration.body ?: return
                eliminateUnusedLocalVariables(body)
            }
        }, null)
    }


    private fun eliminateUnusedLocalVariables(body: IrBody) {
        // Step 1: collect all IrVariables with null initializers declared in this body
        val candidates = mutableSetOf<IrVariable>()
        body.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)
            override fun visitVariable(declaration: IrVariable) {
                if (declaration.initializer == null) candidates += declaration
                declaration.acceptChildrenVoid(this)
            }
        })
        if (candidates.isEmpty()) return

        // Step 2: collect all IrGetValue usages in the body
        val usedSymbols = mutableSetOf<IrValueSymbol>()
        body.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)
            override fun visitGetValue(expression: IrGetValue) {
                usedSymbols += expression.symbol
            }
        })

        // Step 3: remove candidates whose symbol is never read
        val toRemove = candidates.filter { it.symbol !in usedSymbols }.toSet()
        if (toRemove.isEmpty()) return

        body.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitBlock(expression: IrBlock): IrExpression {
                expression.transformChildrenVoid(this)
                expression.statements.removeAll { it is IrVariable && it in toRemove }
                return expression
            }

            override fun visitComposite(expression: IrComposite): IrExpression {
                expression.transformChildrenVoid(this)
                expression.statements.removeAll { it is IrVariable && it in toRemove }
                return expression
            }

            override fun visitBlockBody(body: IrBlockBody): IrBody {
                body.transformChildrenVoid(this)
                body.statements.removeAll { it is IrVariable && it in toRemove }
                return body
            }
        })
    }

}
