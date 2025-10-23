/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id

import org.jetbrains.kotlin.KtFakePsiSourceElement
import org.jetbrains.kotlin.SuspiciousFakeSourceCheck
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId

/**
 * A [FirSymbolId] based on a [KtFakePsiSourceElement].
 *
 * There are cases where the source element of a symbol is a synthetic element *based on* a real PSI element. For example, implicit primary
 * constructors do not have a real PSI element. Instead, they have a fake source element with the kind
 * [KtFakeSourceElementKind.ImplicitConstructor][org.jetbrains.kotlin.KtFakeSourceElementKind.ImplicitConstructor].
 *
 * Such a source element cannot be covered by [LLRealPsiSymbolId] because the symbol ID would only consider the *class* PSI element. If we
 * tried to restore the symbol for the implicit primary constructor, we would restore the class symbol instead. Additionally, the primary
 * constructor symbol ID would also be equal to the class symbol ID.
 *
 * [LLFakePsiSymbolId] considers not only the PSI element, but also the fake source element kind through [KtFakePsiSourceElement]. For
 * example, the implicit primary constructor symbol ID would be clearly marked as the implicit constructor, making it different from its
 * class's symbol ID. In addition, [LLFakePsiSymbolId] also allows implementing custom restoration logic specifically for fake source
 * elements.
 *
 * As an alternative solution, it would also be possible to store [KtPsiSourceElement][org.jetbrains.kotlin.KtPsiSourceElement] in
 * [LLRealPsiSymbolId], but this would increase the memory usage of the symbol ID and add another layer of indirection.
 */
@OptIn(SuspiciousFakeSourceCheck::class)
internal class LLFakePsiSymbolId<S : FirBasedSymbol<*>>(
    private val sourceElement: KtFakePsiSourceElement,
) : FirSymbolId<S>() {
    /**
     * TODO (marco): Note that we use a strong reference here for now to avoid having to write logic for restoring fake source elements for
     *  now. For example, the primary constructor of a class might be a fake source element. AFAIK, it's not possible to easily restore that
     *  just with `resolveToFirSymbol`. Keeping a strong reference removes the need to restore the symbol, while still allowing multiple
     *  instances of the same symbol to coexist.
     *  If these strong references become a problem (see "Problems with High Connectivity" in the design document), we can implement
     *  restoration logic for certain fake elements.
     */
    private lateinit var _symbol: S

    override val symbol: S
        get() = _symbol

    @FirImplementationDetail
    override fun bind(symbol: S) {
        _symbol = symbol
    }

    // TODO (marco): The equality/hash code implementation of `KtFakePsiSourceElement` might not be bulletproof, so we should double-check
    //  them. This is the same as with `LLRealPsiSymbolId`.
    override fun equals(other: Any?): Boolean = other is LLFakePsiSymbolId<*> && sourceElement == other.sourceElement

    override fun hashCode(): Int = sourceElement.hashCode()
}
