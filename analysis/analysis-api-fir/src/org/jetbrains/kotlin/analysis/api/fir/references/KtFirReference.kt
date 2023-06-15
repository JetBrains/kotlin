/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtSymbolBasedReference
import org.jetbrains.kotlin.analysis.api.fir.findReferencePsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.navigationTargetsProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleStructureInternals
import org.jetbrains.kotlin.psi.KtFile

interface KtFirReference : KtReference, KtSymbolBasedReference {
    fun getResolvedToPsi(analysisSession: KtAnalysisSession, referenceTargetSymbols: Collection<KtSymbol>): Collection<PsiElement> =
        with(analysisSession) {
            referenceTargetSymbols.flatMap { symbol ->
                when (symbol) {
                    is KtFirSymbol<*> -> getPsiDeclarations(symbol)
                    else -> listOfNotNull(symbol.psi)
                }
            }
        }

    fun getResolvedToPsi(analysisSession: KtAnalysisSession): Collection<PsiElement> =
        with(analysisSession) {
            getResolvedToPsi(analysisSession, resolveToSymbols())
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
        return firSymbol.fir.findReferencePsi()
    }

    override val resolver get() = KtFirReferenceResolver
}