/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf

abstract class WasmFunctionInlining(
    override val context: WasmBackendContext,
    inlineFunctionResolver: InlineFunctionResolver,
) : FunctionInlining(context, inlineFunctionResolver) {

    // Track for containers and maintain a list of variables to be removed.
    private val containerStack = ArrayDeque<Pair<IrContainerExpression, MutableList<IrVariable>>>()

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration): IrExpression {

        val symbol = expression.symbol
        if (!symbol.isBound) return super.visitFunctionAccess(expression, data)

        val realOwner = symbol.owner.resolveFakeOverrideOrSelf()
        if (realOwner == context.symbols.suspendCoroutineUninterceptedOrReturnIntrinsic.owner) {
            expression.arguments[0] = unwrapTemporaryVariableChain(expression.arguments[0])
        }

        return super.visitFunctionAccess(expression, data)
    }

    // Eliminate IrGetValue chains for temporary variables storing the value of `block` passed to `suspendCoroutineUninterceptedOrReturn`.
    // Replace temporary variable passed to `suspendCoroutineUninterceptedOrReturnIntrinsic` with the actual block that will be inlined.
    private fun unwrapTemporaryVariableChain(expression: IrExpression?): IrExpression? {
        var current = expression
        while (current is IrGetValue) {
            val temporary = current.symbol.owner as? IrVariable ?: break
            current = temporary.initializer
            addVariableToRemovalList(temporary)
        }
        return current
    }

    // Find the container of a variable and add it to container's removal list (usually only ~1-2 iterations).
    private fun addVariableToRemovalList(variable: IrVariable) {
        val it = containerStack.listIterator(containerStack.size)
        while (it.hasPrevious()) {
            val prev = it.previous()
            if (prev.first.statements.contains(variable)) {
                prev.second.add(variable)
                return
            }
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: IrDeclaration): IrExpression {
        containerStack.addLast(expression to mutableListOf())
        try {
            super.visitContainerExpression(expression, data)
        } finally {
            containerStack.removeLast().let { [container, vars] ->
                if (vars.isNotEmpty()) {
                    container.statements.removeAll(vars)
                }
            }
        }
        return expression
    }
}

internal class WasmPrivateFunctionInlining(context: WasmBackendContext) : WasmFunctionInlining(
    context,
    WasmInlineFunctionResolver(context, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS),
)

internal class WasmAllFunctionInlining(context: WasmBackendContext) : WasmFunctionInlining(
    context,
    WasmInlineFunctionResolver(context, inlineMode = InlineMode.ALL_INLINE_FUNCTIONS),
)
