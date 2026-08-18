/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.wasm.BackendWasmSymbols
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class WasmCoroutinesSymbolsResolver(context: WasmBackendContext) : BodyLoweringPass {

    private val stackSwitchingIntrinsicsTransformer = context.wasmSymbols.coroutinesStackSwitchingIntrinsics?.let {
        WasmCoroutinesStackSwitchingIntrinsicsTransformer(context.wasmSymbols, it)
    }

    override fun lower(irModule: IrModuleFragment) {
        if (stackSwitchingIntrinsicsTransformer != null) {
            super.lower(irModule)
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(stackSwitchingIntrinsicsTransformer!!)
    }
}

private class WasmCoroutinesStackSwitchingIntrinsicsTransformer(
    private val wasmSymbols: BackendWasmSymbols,
    private val stackSwitchingIntrinsics: BackendWasmSymbols.CoroutinesStackSwitchingIntrinsics,
) : IrElementTransformerVoid() {

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        val symbol = expression.symbol
        if (!symbol.isBound) return expression

        val realOwner = symbol.owner.resolveFakeOverrideOrSelf()

        return when (realOwner.symbol) {
            wasmSymbols.suspendCoroutineUninterceptedOrReturnIntrinsic ->
                irCall(expression, stackSwitchingIntrinsics.suspendCoroutineUninterceptedOrReturnIntrinsicStackSwitching)
            wasmSymbols.createCoroutineUninterceptedIntrinsic0 ->
                irCall(expression, stackSwitchingIntrinsics.createCoroutineUninterceptedIntrinsic0StackSwitching)
            wasmSymbols.createCoroutineUninterceptedIntrinsic1 ->
                irCall(expression, stackSwitchingIntrinsics.createCoroutineUninterceptedIntrinsic1StackSwitching)
            else -> expression
        }
    }
}
