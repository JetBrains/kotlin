/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.id

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * TODO (marco): Document. Symbol lifetime, role of the symbol ID for equality and restoration, unique vs. source-based symbol IDs, etc.
 * TODO (marco): Naming: `FirIdentitySymbolId`?
 * TODO (marco): Attach `@FirImplementationDetail` or `internal`?
 * TODO (marco): Move to `id` package.
 */
class FirUniqueSymbolId<S : FirBasedSymbol<*>> : FirSymbolId<S>() {
    // TODO (marco): NOT volatile. `bind` needs to guarantee that the symbol ID isn't published cross-thread until the binding is finished.
    private var _symbol: S? = null

    override val symbol: S
        get() = _symbol ?: error("Symbol for $this has not been bound yet") // TODO (marco): Better error handling?

    @FirImplementationDetail
    override fun bind(symbol: @UnsafeVariance S) {
        _symbol = symbol
    }

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
