/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id

import org.jetbrains.kotlin.KtFakePsiSourceElement
import org.jetbrains.kotlin.SuspiciousFakeSourceCheck
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
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
 *
 * As with [LLRealPsiSymbolId], the session is part of the symbol ID's equality.
 */
@OptIn(SuspiciousFakeSourceCheck::class)
internal class LLFakePsiSymbolId<S : FirBasedSymbol<*>>(
    private val session: LLFirSession,
    private val sourceElement: KtFakePsiSourceElement,
) : FirSymbolId<S>() {
    /**
     * The symbol ID references its symbol strongly for now, so it never has to restore it. Restoring a symbol from a fake source element
     * requires custom logic: for example, an implicit primary constructor cannot be restored with `resolveToFirSymbol`, because its real
     * PSI element is the class. Should these strong references become a problem for memory usage, restoration logic can be implemented
     * for specific fake source element kinds (KT-90475).
     */
    private lateinit var _symbol: S

    override val symbol: S
        get() = _symbol

    @FirImplementationDetail
    override fun bind(symbol: S) {
        _symbol = symbol
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
                other is LLFakePsiSymbolId<*> &&
                session == other.session &&
                sourceElement == other.sourceElement

    override fun hashCode(): Int = sourceElement.hashCode()
}
