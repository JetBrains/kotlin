/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.suspendCoroutineUninterceptedOrReturnIntrinsicByMode
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf

class WasmInlineFunctionResolver(
    private val context: WasmBackendContext,
    private val inlineMode: InlineMode,
) : InlineFunctionResolver() {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (!symbol.isBound) return null
        val realOwner = symbol.owner.resolveFakeOverrideOrSelf()

        val substituteSuspendCoroutineIntrinsic =
            realOwner.symbol == context.symbols.suspendCoroutineUninterceptedOrReturnIntrinsic

        val result = when {
            substituteSuspendCoroutineIntrinsic -> context.suspendCoroutineUninterceptedOrReturnIntrinsicByMode.owner
            realOwner.isInline -> realOwner
            else -> return null
        }
        if (inlineMode == InlineMode.PRIVATE_INLINE_FUNCTIONS && !result.isEffectivelyPrivate()) return null
        return result
    }
}
