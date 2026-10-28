/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.id

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * An exact identifier for a [FirBasedSymbol] and its FIR declaration.
 *
 * A symbol ID has two roles:
 *
 * - **Equality:** Symbol IDs have equality semantics, so they can be used as cache keys which identify a symbol independently of its
 *   instance. Two equal symbol IDs always identify the same declaration. Symbols and declarations themselves currently have identity
 *   equality (see [FirBasedSymbol]).
 * - **Reference:** A symbol ID references its [symbol]. Depending on the kind of symbol ID, the reference may be weak, in which case the
 *   symbol ID restores the symbol on demand after it has been discarded. Since symbol IDs may represent resolution results, the
 *   restoration must be exact.
 *
 * Together, these roles allow cross-references and caches to identify a symbol without keeping it alive.
 *
 * A symbol binds itself to its symbol ID on construction (see [bind]). Symbol IDs are created by the session's [FirSymbolIdFactory], or by
 * the default constructors of symbols (see [FirBasedSymbol]).
 *
 * ### Unique and source-based symbol IDs
 *
 * There are two main kinds of symbol IDs:
 *
 * - **Unique symbol IDs** ([FirUniqueSymbolId]) preserve the traditional identity semantics of symbols: each symbol has its own symbol ID,
 *   which is only equal to itself. A unique symbol ID references its symbol strongly.
 * - **Source-based symbol IDs** are based on the [KtSourceElement][org.jetbrains.kotlin.KtSourceElement] of the declaration, which defines
 *   their equality. Whether a source-based symbol ID references its symbol strongly or weakly depends on its implementation. Source-based
 *   symbol IDs are only created in the Analysis API.
 *
 * A source-based symbol ID is thus not tied to the identity of a single symbol instance. If a symbol is discarded and later recreated (for
 * example, when a discarded FIR file is rebuilt), the recreated symbol has an equal symbol ID.
 *
 * ### Discardable declarations
 *
 * A FIR declaration with a source-based symbol ID is *discardable*: it may be discarded and later recreated. This is only correct if the
 * recreation is *reproducible*, i.e. if it yields equal symbol IDs for the same declarations, so that existing references and cache keys
 * remain valid.
 *
 * Discardable declarations thus have to satisfy two symbol ID constraints:
 *
 * 1. **Source-based symbol IDs:** Any child declaration of a discardable declaration must have a source-based symbol ID, including local
 *    declarations, i.e. it must be discardable as well.
 * 2. **Distinct symbol IDs:** The symbol IDs of different declarations must never be equal.
 *
 * The constraints support both roles of a symbol ID:
 *
 * - **Equality:** For discardable declarations, symbol ID equality coincides with declaration identity. Without constraint 1, recreating a
 *   parent would create its child again with a new unique symbol ID, while the old symbol ID may still be referenced, for example as a
 *   cache key. The same declaration would then have two unequal symbol IDs and two symbols. Without constraint 2, two unrelated
 *   declarations could have equal symbol IDs.
 * - **Reference:** A reference to a discardable declaration or one of its children should not keep the discardable declaration alive, and
 *   restoration should be exact. Constraint 1 extends the source-based symbol ID requirement to child declarations, which ensures that
 *   cross-references and cache keys also reference child symbols weakly. Depending on the lifetime strategy, a strong reference to a child
 *   declaration would either keep the parent root declaration alive or lead to duplicate symbols. Both cases are undesirable for memory
 *   usage. And without constraint 2, restoring a symbol from its symbol ID would be ambiguous.
 *
 * Both constraints are checked on discardable *root declarations*. These are declarations stored directly by a cache as the entry point of
 * a FIR tree, such as a FIR file or a FIR class deserialized from a library.
 *
 * In particular, constraint 2 is also applied to root declarations, instead of a session-wide or project-wide scope. This keeps the
 * constraint easily testable. Overlaps across root declarations are not expected, since different root declarations are usually based on
 * different source files.
 *
 * ### Choosing between unique and source-based symbol IDs
 *
 * Historically, all FIR symbols were unique, and most *kinds* of symbols still have unique symbol IDs. A symbol with a unique symbol ID has
 * to be stored for the lifetime of its session: if it were discarded and recreated, the new symbol ID would not be equal to the old one,
 * so existing references and cache keys would become stale. This is the same requirement which applied to all symbols before symbol IDs
 * were introduced, so unique symbols don't need to be treated differently now.
 *
 * A source-based symbol ID unlocks the *option* to keep a symbol for less than the session lifetime, for example by discarding cached FIR
 * and recreating it later. Making a symbol source-based doesn't break anything by itself, since the session lifetime remains the widest
 * possible lifetime of the symbol. The reverse doesn't hold: a symbol which may be discarded and recreated cannot have a unique symbol ID.
 *
 * The choice thus comes down to whether the declaration needs to be *discardable* (see above):
 *
 * - A declaration which needs to be discardable requires a source-based symbol ID. This is the case if a cache should be able to discard
 *   the declaration, or if the declaration is a child of a discardable declaration (see the first constraint above), which includes local
 *   declarations generated by compiler plugins.
 * - Otherwise, a unique symbol ID is the simpler choice. For example, declarations generated by session components (such as substitution
 *   overrides) and by compiler plugins currently have unique symbol IDs. Should a cache need to discard such declarations, for example to
 *   free up memory, their symbol IDs can be made source-based.
 *
 * A source-based symbol ID requires a source element which identifies the declaration exactly, i.e. no other declaration of the session
 * has an equal source element. A real source element identifies its declaration exactly. For other declarations, a distinct fake source
 * element can be built from an original source element and a fake source element kind which distinguishes the declaration from other
 * declarations based on the same original element. A missing source element is thus not a reason for a unique symbol ID by itself, but
 * building a distinct fake source element takes additional effort.
 */
abstract class FirSymbolId<out S : FirBasedSymbol<*>> {
    /**
     * The symbol identified by this symbol ID.
     *
     * For a unique symbol ID, this is always the symbol bound to it. A source-based symbol ID may have to restore its symbol if it has
     * been discarded, so the result can be a different symbol instance than the one originally bound. It is always a symbol for the same
     * declaration, with an equal symbol ID.
     */
    abstract val symbol: S

    /**
     * Binds [symbol] to this symbol ID, making it the symbol which [FirSymbolId.symbol] returns.
     *
     * Each symbol binds itself to its symbol ID on construction. Source-based symbol IDs may additionally bind a restored symbol.
     */
    @FirImplementationDetail
    abstract fun bind(symbol: @UnsafeVariance S)

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}
