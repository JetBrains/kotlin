/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.isSymbolRestorationSupported
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.restoreSymbol
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.lang.ref.WeakReference

/**
 * A source-based [FirSymbolId] for a declaration with a real PSI element, such as a [KtDeclaration][org.jetbrains.kotlin.psi.KtDeclaration]
 * or a [KtFile][org.jetbrains.kotlin.psi.KtFile]. The supported PSI elements are defined by [isSymbolRestorationSupported].
 *
 * The symbol ID references its symbol weakly. If the symbol has been discarded, the symbol ID restores it from the PSI element in the
 * symbol ID's [session] (see [restoreSymbol]).
 *
 * Two such symbol IDs are equal if they have the same PSI element and the same session. A symbol belongs to exactly one session, so
 * symbols for the same PSI element in different sessions are not equal, in the same way as with unique symbol IDs.
 *
 * ### Why PSI elements
 *
 * Class IDs and callable IDs are not sufficient to identify a declaration exactly, even for top-level declarations:
 *
 * - Classes can be ambiguous, but restoration has to find exactly the same class.
 * - Callables can be overloaded, while the PSI element identifies the exact callable.
 *
 * The symbol ID references the PSI element strongly. Smart PSI pointers would avoid the strong reference, but they are too expensive to
 * create and dereference for the large number of symbol IDs (see KT-57589).
 *
 * ### Why the session
 *
 * The symbol ID stores its [session] because it is the most precise and efficient context for restoration. We could also get the
 * `KaModule` of the PSI element, but this is (1) less performant when we need to restore the symbol and (2) might break in edge cases since
 * we don't have a contextual use-site module, so the `KaModule` of the PSI element might be different from the `KaModule` of the session
 * which originally contained the symbol.
 */
internal class LLRealPsiSymbolId<S : FirBasedSymbol<*>>(
    private val session: LLFirSession,
    val psi: PsiElement,
) : FirSymbolId<S>() {
    /**
     * A weak reference to the bound symbol.
     *
     * In practice, the symbol stays strongly reachable for a long time: until the FIR file and possibly symbol provider caches have
     * discarded it. So the symbol ID has to restore its symbol less often than the weak reference might suggest.
     */
    private var symbolReference: WeakReference<S>? = null

    init {
        // We only need to care about the specific PSI element when restoring a symbol, but we should check whether it's supported during
        // symbol ID creation since an error will be much more visible. In contrast to symbol ID creation, restoration will only happen in
        // specific cases after symbols were garbage collected.
        if (!psi.isSymbolRestorationSupported) {
            errorWithAttachment("The PSI element of an `${LLRealPsiSymbolId::class.simpleName}` is not supported.") {
                withEntry("psiClass", psi::class.simpleName)
            }
        }
    }

    override val symbol: S
        get() = symbolReference?.get() ?: restoreSymbol()

    @FirImplementationDetail
    override fun bind(symbol: S) {
        symbolReference = WeakReference(symbol)
    }

    @OptIn(FirImplementationDetail::class)
    private fun restoreSymbol(): S {
        @Suppress("UNCHECKED_CAST")
        val newSymbol = session.restoreSymbol(psi) as S

        bind(newSymbol)
        return newSymbol
    }

    // TODO KT-90476: PSI identity might not be stable for the whole lifetime of the symbol ID, e.g. after reparses.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LLRealPsiSymbolId<*>) return false

        // `PsiManager.areElementsEquivalent` is not an alternative to PSI identity, since it considers different classes with the same FQ
        // name equivalent.
        return session == other.session && psi == other.psi
    }

    override fun hashCode(): Int = psi.hashCode()
}
