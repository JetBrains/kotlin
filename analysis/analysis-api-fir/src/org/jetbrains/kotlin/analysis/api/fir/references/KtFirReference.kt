/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaSymbolBasedReference
import org.jetbrains.kotlin.analysis.api.fir.findReferencePsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin

interface KaFirReference : KtReference, KaSymbolBasedReference {
    fun getResolvedToPsi(analysisSession: KaSession, referenceTargetSymbols: Collection<KaSymbol>): Collection<PsiElement> =
        with(analysisSession) {
            referenceTargetSymbols.flatMap { symbol ->
                when (symbol) {
                    is KaFirSymbol<*> -> getPsiDeclarations(symbol)
                    else -> listOfNotNull(symbol.psi)
                }
            }
        }

    fun getResolvedToPsi(analysisSession: KaSession): Collection<PsiElement> =
        with(analysisSession) {
            getResolvedToPsi(analysisSession, resolveToSymbols())
        }

    override val resolver get() = KaFirReferenceResolver
}

internal fun KaSession.getPsiDeclarations(symbol: KaFirSymbol<*>): Collection<PsiElement> {
    val intersectionOverriddenSymbolsOrSingle = when {
        symbol.origin == KaSymbolOrigin.INTERSECTION_OVERRIDE && symbol is KaCallableSymbol -> symbol.getIntersectionOverriddenSymbols()
        else -> listOf(symbol)
    }
    return intersectionOverriddenSymbolsOrSingle.mapNotNull { it.findPsiForReferenceResolve() }
}

private fun KaSymbol.findPsiForReferenceResolve(): PsiElement? {
    require(this is KaFirSymbol<*>)
    return firSymbol.fir.findReferencePsi()
}
