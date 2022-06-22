/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import gnu.trove.THashSet
import gnu.trove.TObjectByteHashMap
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
    private val symbols = TObjectByteHashMap<IrClassifierSymbol>()
    private val patchedSymbols = THashSet<IrClassSymbol>() // To avoid re-patching what already has been patched.

    fun forEachClassSymbolToPatch(patchAction: (IrClassSymbol) -> Unit) {
        symbols.forEachEntry { symbol, code ->
            if (symbol.isBound && code.status == UNLINKED && symbol is IrClassSymbol && patchedSymbols.add(symbol)) {
                patchAction(symbol)
            }
            true
        }
    }

    operator fun get(symbol: IrClassifierSymbol): UsedClassifierSymbolStatus? = symbols[symbol].status

    fun register(symbol: IrClassifierSymbol, status: UsedClassifierSymbolStatus): Boolean {
        symbols.put(symbol, status.code)
        return status.isUnlinked
    }

    companion object {
        private inline val Byte.status: UsedClassifierSymbolStatus?
            get() = when (this) {
                1.toByte() -> UNLINKED
                2.toByte() -> LINKED
                else -> null
            }

        private inline val UsedClassifierSymbolStatus.code: Byte
            get() = when (this) {
                UNLINKED -> 1.toByte()
                LINKED -> 2.toByte()
            }
    }
}
