/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import java.lang.ref.WeakReference

/**
 * TODO (marco): Document.
 *
 * TODO (marco): Why we cannot even use class IDs/callable IDs (instead of a heavier PSI element) for top-level declarations:
 *  - Classes: We can have ambiguities, and we need to restore exactly the same class (by PSI).
 *  - Callables: We can have overloads, and the PSI itself is the best way to find the exact callable.
 *  Also note: We cannot use symbol pointers because their creation is too expensive for the mass of pointers we need to create. Link the
 *  declined symbol pointer issue.
 *
 * NOTE: We pass the [session] directly because it is the most precise and efficient context for restoration. We could also get the
 * `KaModule` of the PSI element, but this is (1) less performant when we need to restore the symbol and (2) might break in edge cases since
 * we don't have a contextual use-site module, so the `KaModule` grabbed from the PSI element might be different than the `KaModule` of the
 * initially containing session.
 */
internal class LLRealPsiSymbolId<S : FirBasedSymbol<*>>(
    private val session: LLFirSession,
    val psi: PsiElement,
) : FirSymbolId<S>() {
    /**
     * TODO (marco): Document that the lifetime of the weak reference is actually quite long, until the FIR file AND possibly symbol
     *  provider caches are thrown away for the specific symbol. So the reference will be more long-living than we can initially assume.
     */
    private var symbolReference: WeakReference<S>? = null

    init {
        // We only need to care about the specific PSI element when restoring a symbol, but we should check whether it's supported during
        // symbol ID creation since an error will be much more visible. In contrast to symbol ID creation, restoration will only happen in
        // specific cases after symbols were garbage collected.
        if (!psi.isSupported) {
            notSupportedError(psi)
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
        val newSymbol = resolveSymbol()

        bind(newSymbol)
        return newSymbol
    }

    @OptIn(FirImplementationDetail::class)
    private fun resolveSymbol(): S {
        val module = session.ktModule
        val resolutionFacade = module.getResolutionFacade(module.project)

        // The `when` should be kept in sync with `isSupported`.
        return when (val psi = psi) {
            is KtDeclaration -> {
                // TODO (marco): Going through the resolution facade surely isn't 100% optimal. If we need better performance here, we
                //  can write a specialized version of the function. For example, we know that `ktDeclaration` is from
                //  `symbolId.session`, so the `getModule` call inside `resolveToFirSymbol` is nonsense.
                // Note: `resolveToFirSymbol` supports local declarations as well, so we can also restore local symbols!
                @Suppress("UNCHECKED_CAST")
                resolutionFacade.resolveToFirSymbol(psi, FirResolvePhase.RAW_FIR) as S
            }

            is KtFile -> {
                @Suppress("UNCHECKED_CAST")
                resolutionFacade.getOrBuildFirFile(psi).symbol as S
            }

            else -> notSupportedError(psi)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LLRealPsiSymbolId<*>) return false

        // TODO (marco): Can we use object identity given that the FIR lives in an immutable world?
        // TODO (marco): We CANNOT use `areElementsEquivalent` here because it will return `true` for DIFFERENT classes with the same FQ
        //  name. Ouch!
//        return PsiManager.getInstance(psi.project).areElementsEquivalent(psi, other.psi)
        return psi == other.psi
    }

    override fun hashCode(): Int {
        // TODO (marco): This is not recommended across reparses. Can we rely on this since the FIR lives in an immutable world? Or are
        //  AST loads already killing this?
        //  As an alternative, we would have to consider virtual file + offset or something similar. Maybe ask Anna Kozlova or in IJ dev.
        return psi.hashCode()
    }

    companion object {
        /**
         * Whether the PSI element is supported by [LLRealPsiSymbolId]. The symbol ID can only restore symbols for specific types of PSI
         * elements.
         */
        // TODO (marco): Support `PsiClass` specifically so that we can restore Java PSI class symbols.
        private val PsiElement.isSupported: Boolean
            get() = this is KtDeclaration || this is KtFile

        private fun notSupportedError(psi: PsiElement): Nothing = error(
            "The PSI element of a `${LLRealPsiSymbolId::class.simpleName}` should be a `${KtDeclaration::class.simpleName}`, but it" +
                    " was a `${psi::class.simpleName}`."
        )
    }
}
