/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.findReferencePsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.isConstructorOf
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

@OptIn(KtImplementationDetail::class)
internal sealed interface KaFirReference : KtReference {
    fun KaSession.resolveToSymbols(): Collection<KaSymbol>

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

    fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return getImportAlias(alias.importDirective) != null
    }

    fun getImportAlias(importDirective: KtImportDirective?): KtImportAlias? {
        val importedReference = importDirective?.importedReference ?: return null
        val importResults =
            when (importedReference) {
                is KtDotQualifiedExpression -> importedReference.selectorExpression?.mainReference?.multiResolve(false)
                is KtSimpleNameExpression -> importedReference.mainReference.multiResolve(false)
                else -> null
            } ?: return null
        val targets = multiResolve(false).mapNotNull { it.element }
        val adjustedImportTargets = importResults.mapNotNull { it.element }
        val manager = importDirective.manager
        if (adjustedImportTargets.any { importTarget ->
                targets.any { target ->
                    manager.areElementsEquivalent(target, importTarget) ||
                            target.isConstructorOf(importTarget) ||
                            importTarget is KtObjectDeclaration && importTarget.isCompanion() && importTarget.getNonStrictParentOfType<KtClass>() == target
                }
            }) {
            return importDirective.alias
        }
        return null
    }
}

internal fun KaSession.getPsiDeclarations(symbol: KaFirSymbol<*>): Collection<PsiElement> {
    val intersectionOverriddenSymbolsOrSingle = when {
        symbol.origin == KaSymbolOrigin.INTERSECTION_OVERRIDE && symbol is KaCallableSymbol -> symbol.intersectionOverriddenSymbols
        else -> listOf(symbol)
    }
    return intersectionOverriddenSymbolsOrSingle.mapNotNull { it.findPsiForReferenceResolve(analysisScope) }
}

private fun KaSymbol.findPsiForReferenceResolve(scope: GlobalSearchScope): PsiElement? {
    require(this is KaFirSymbol<*>)
    return firSymbol.fir.findReferencePsi(scope)
}
