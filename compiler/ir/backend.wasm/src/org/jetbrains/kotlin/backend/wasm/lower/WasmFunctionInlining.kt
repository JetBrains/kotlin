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
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf

abstract class WasmFunctionInlining(
    override val context: WasmBackendContext,
    inlineFunctionResolver: InlineFunctionResolver,
) : FunctionInlining(context, inlineFunctionResolver) {
    private val deadTempBlockVars = hashSetOf<IrVariableSymbol>()

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration): IrExpression {

        val symbol = expression.symbol
        if (!symbol.isBound) return super.visitFunctionAccess(expression, data)

        val realOwner = symbol.owner.resolveFakeOverrideOrSelf()
        if (realOwner == context.symbols.suspendCoroutineUninterceptedOrReturnIntrinsic.owner) {
            var blockParameter: IrExpression? = expression.arguments[0]
            while (blockParameter is IrGetValue) {
                val blockIrTemporary = blockParameter.symbol.owner as? IrVariable
                if (blockIrTemporary != null) {
                    blockParameter = blockIrTemporary.initializer
                    deadTempBlockVars += blockIrTemporary.symbol
                } else {
                    break
                }
            }
            expression.arguments[0] = blockParameter
        }

        return super.visitFunctionAccess(expression, data)
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: IrDeclaration): IrExpression {
        super.visitContainerExpression(expression, data)
        if (deadTempBlockVars.isNotEmpty()) {
            expression.statements.removeAll { it is IrVariable && it.symbol in deadTempBlockVars }
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
