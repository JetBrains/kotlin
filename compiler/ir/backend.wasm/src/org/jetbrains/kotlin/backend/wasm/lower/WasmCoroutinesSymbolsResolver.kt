/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class WasmCoroutinesSymbolsResolver(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(WasmCoroutinesIntrinsicsTransformer(context))
    }
}

private class WasmCoroutinesIntrinsicsTransformer(val context: WasmBackendContext) :
    IrElementTransformerVoidWithContext() {
    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        val symbol = expression.symbol
        if (!symbol.isBound) return expression
        val realOwner = symbol.owner.resolveFakeOverrideOrSelf()
        val createCoroutineSymbols = context.wasmSymbols.createCoroutineUninterceptedIntrinsicsGeneral
        if (realOwner.symbol == context.wasmSymbols.createSimpleCoroutineFromSuspendFunctionGeneral) {
            return irCall(expression, context.wasmSymbols.createSimpleCoroutineFromSuspendFunction)
        } else if (realOwner.symbol in createCoroutineSymbols) {
            val idx = createCoroutineSymbols.indexOf(realOwner.symbol)
            return irCall(expression, context.wasmSymbols.createCoroutineUninterceptedIntrinsics[idx])
        }
        return expression
    }
}