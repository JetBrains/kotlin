/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.id

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * TODO (marco): Document. Highlight the non-identity aspect and the use as an equality token.
 *
 * Why not `FirSymbolPointer`: The symbol ID is also used as an equality token, which is not the point of a pointer.
 *
 * Also note: Historically, all FIR symbols were unique. This is still generally the default for most kinds of FIR symbols. As such, even
 * with the introduction of symbol IDs, there's no need to treat unique symbols differently now (for example to change their storage modality).
 * Unique symbols always required proper
 * storage to ensure their lifetime. When we make a symbol non-unique, we unlock additional **options** to reduce the lifetime of the symbol
 * to less than the session lifetime. This might mean throwing away cached symbols, which in essence is a reduction of storage lifetime.
 * However, no functionality breaks immediately when we make a symbol source-based. The session lifetime is its widest possible lifetime
 * and there by default. Problems only occur when turning a source-based symbol into a unique symbol, which is not legal without further
 * modifications.
 *
 * Include a clear guide when a symbol can be unique vs. when it cannot.
 */
abstract class FirSymbolId<out S : FirBasedSymbol<*>> {
    /**
     * TODO (marco): Document.
     */
    abstract val symbol: S

    /**
     * TODO (marco): Document.
     */
    @FirImplementationDetail
    abstract fun bind(symbol: @UnsafeVariance S)

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}
