/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.lower.JsExceptionRevealOrigin.Companion.JS_EXCEPTION_REVEAL
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

/**
 * Wraps try block with finalizer and/or catch block for Throwable/JsException into JS reveal intrinsic
 *
 * try {
 *   foo()
 * } catch(e: JsExpression) {
 *   bar()
 * }
 *
// converts into
 *
 * try {
 *   composite { foo() } with origin JS_EXCEPTION_REVEAL
 * } catch(e: JsExpression) {
 *   bar()
 * }
 */

interface JsExceptionRevealOrigin : IrStatementOrigin {
    companion object {
        val JS_EXCEPTION_REVEAL by IrStatementOriginImpl
    }
}

class JsExceptionRevealLowering(private val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (context.isWasmJsTarget && !context.configuration.getBoolean(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS)) {
            irBody.transformChildrenVoid(JsExceptionRevealTransformer(context, container.symbol))
        }
    }

    private class JsExceptionRevealTransformer(
        val context: WasmBackendContext,
        val containerSymbol: IrSymbol,
    ) : IrElementTransformerVoidWithContext() {

        private fun needToReveal(aTry: IrTry): Boolean {
            if (aTry.finallyExpression != null) return true
            val throwableType = context.irBuiltIns.throwableType
            val jsExceptionType = context.wasmSymbols.jsRelatedSymbols.jsException.defaultType
            return aTry.catches.any {
                it.catchParameter.type.let { it == throwableType || it == jsExceptionType }
            }
        }

        override fun visitTry(aTry: IrTry): IrExpression {
            aTry.transformChildrenVoid(this)

            if (!needToReveal(aTry)) return aTry

            context.createIrBuilder(containerSymbol).run {
                aTry.tryResult = irComposite(
                    resultType = aTry.tryResult.type,
                    origin = JS_EXCEPTION_REVEAL
                ) {
                    +aTry.tryResult
                }
            }
            return aTry
        }
    }
}