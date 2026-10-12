/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.id

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolIdEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * A [FirSymbolId] based on identity. It preserves the traditional identity semantics of symbols: each symbol has its own unique symbol ID,
 * which is only equal to itself.
 *
 * The symbol ID is unique in the sense that it is the only symbol ID of its declaration. In contrast, a source-based symbol ID may exist
 * in multiple equal instances, even if there is only one *symbol* instance at a time.
 *
 * A unique symbol ID references its symbol strongly and cannot restore it. Since a recreated symbol would get a new, unequal symbol ID, a
 * symbol with a unique symbol ID has to be stored for the lifetime of its session (see [FirSymbolId]). The symbol ID is bound exactly
 * once, by the constructor of its symbol.
 */
class FirUniqueSymbolId<S : FirBasedSymbol<*>> : FirSymbolId<S>() {
    // `_symbol` doesn't need to be volatile: the symbol binds itself in its constructor and references this symbol ID via a final field,
    // so final field semantics guarantee that any thread which accesses the symbol ID through its symbol also sees the bound symbol. This
    // only breaks if the symbol ID is shared with other threads before its symbol is constructed.
    private var _symbol: S? = null

    override val symbol: S
        get() = _symbol ?: errorWithAttachment("The symbol of a unique symbol ID has not been bound yet") {
            withEntry("symbolId", this@FirUniqueSymbolId.toString())
        }

    @FirImplementationDetail
    override fun bind(symbol: @UnsafeVariance S) {
        val boundSymbol = _symbol
        if (boundSymbol != null) {
            errorWithAttachment("A unique symbol ID cannot be bound to multiple symbols") {
                withFirSymbolIdEntry("boundSymbol", boundSymbol)

                // `bind` is called during the construction of the new symbol, so its properties might not be initialized yet.
                withEntry("newSymbolClass", symbol::class.simpleName)
            }
        }

        _symbol = symbol
    }

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
