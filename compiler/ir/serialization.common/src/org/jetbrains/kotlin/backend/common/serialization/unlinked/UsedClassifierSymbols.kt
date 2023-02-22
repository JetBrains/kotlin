/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.UsedClassifierSymbolStatus.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol

internal enum class UsedClassifierSymbolStatus(val isUnlinked: Boolean) {
    /** IR symbol of unlinked classifier. */
    UNLINKED(true),

    /** IR symbol of linked classifier. */
    LINKED(false);

    companion object {
        val UsedClassifierSymbolStatus?.isUnlinked: Boolean get() = this?.isUnlinked == true
    }
}

internal class UsedClassifierSymbols {
    private val symbols = HashMap<IrClassifierSymbol, Boolean>()
    private val patchedSymbols = HashSet<IrClassSymbol>() // To avoid re-patching what already has been patched.

    fun forEachClassSymbolToPatch(patchAction: (IrClassSymbol) -> Unit) {
        symbols.forEach { (symbol, isUnlinked) ->
            if (isUnlinked && symbol.isBound && symbol is IrClassSymbol && patchedSymbols.add(symbol)) {
                patchAction(symbol)
            }
        }
    }

    operator fun get(symbol: IrClassifierSymbol): UsedClassifierSymbolStatus? =
        when (symbols[symbol]) {
            true -> UNLINKED
            false -> LINKED
            null -> null
        }

    fun register(symbol: IrClassifierSymbol, status: UsedClassifierSymbolStatus): Boolean {
        symbols[symbol] = status.isUnlinked
        return status.isUnlinked
    }
}
