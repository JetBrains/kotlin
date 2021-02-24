/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.findReferencePsi
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtSymbolBasedReference
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolOrigin

interface KtFirReference : KtReference, KtSymbolBasedReference {
    fun getResolvedToPsi(analysisSession: KtAnalysisSession): Collection<PsiElement> = with(analysisSession) {
        resolveToSymbols().flatMap { symbol ->
            when (symbol) {
                is KtFirSymbol<*> -> getPsiDeclarations(symbol)
                else -> listOfNotNull(symbol.psi)
            }
        }
    }

    private fun KtAnalysisSession.getPsiDeclarations(symbol: KtFirSymbol<*>): Collection<PsiElement> {
        val intersectionOverriddenSymbolsOrSingle = when {
            symbol.origin == KtSymbolOrigin.INTERSECTION_OVERRIDE && symbol is KtCallableSymbol -> symbol.getIntersectionOverriddenSymbols()
            else -> listOf(symbol)
        }
        return intersectionOverriddenSymbolsOrSingle.mapNotNull { it.findPsiForReferenceResolve() }
    }

    private fun KtSymbol.findPsiForReferenceResolve(): PsiElement? {
        require(this is KtFirSymbol<*>)
        return firRef.withFir { it.findReferencePsi() }
    }

    override val resolver get() = KtFirReferenceResolver
}