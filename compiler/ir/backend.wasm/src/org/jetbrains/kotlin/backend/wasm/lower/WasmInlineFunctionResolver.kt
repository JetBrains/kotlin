/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.suspendCoroutineUninterceptedOrReturnByMode
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf

class WasmInlineFunctionResolver(
    private val context: WasmBackendContext,
    private val inlineMode: InlineMode,
) : InlineFunctionResolver() {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (!symbol.isBound) return null
        val realOwner = symbol.owner.resolveFakeOverrideOrSelf()
        if (!realOwner.isInline) return null

        // Older klibs (before 2.5.0) contain `getCoroutineContext` and  non-inlined.
        // In newer klibs for K/Wasm (2.5.0 and after), `getCoroutineContext` is inlined
        // and its usages replaced with usages of `getCoroutineContextImpl`.
        // See: libraries/stdlib/wasm/internal/kotlin/wasm/internal/Coroutines.kt

        val isCoroutineContextGetter =
            realOwner.symbol == context.symbols.coroutineContextGetter || realOwner.symbol == context.symbols.coroutineGetContext

        val result = when {
            realOwner.isBuiltInSuspendCoroutineUninterceptedOrReturn() -> context.suspendCoroutineUninterceptedOrReturnByMode.owner
            isCoroutineContextGetter -> context.symbols.getCoroutineContextImpl.owner
            else -> realOwner
        }
        if (inlineMode == InlineMode.PRIVATE_INLINE_FUNCTIONS && !result.isEffectivelyPrivate()) return null
        return result
    }
}
