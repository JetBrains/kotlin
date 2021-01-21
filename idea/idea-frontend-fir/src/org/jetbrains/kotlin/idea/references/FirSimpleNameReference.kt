/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

internal class KtFirSimpleNameReference(
    expression: KtSimpleNameExpression
) : KtSimpleNameReference(expression), KtFirReference {

    private fun KtAnalysisSession.fixUpAnnotationCallResolveToCtor(resultsToFix: Collection<KtSymbol>): Collection<KtSymbol> {
        if (resultsToFix.isEmpty()) return resultsToFix

        val isAnnotationCall = (((expression.parent as? KtUserType)
            ?.parent as? KtTypeReference)
            ?.parent as? KtConstructorCalleeExpression)
            ?.parent is KtAnnotationEntry
        if (!isAnnotationCall) return resultsToFix

        return resultsToFix.map { targetSymbol ->
            if (targetSymbol is KtFirClassOrObjectSymbol && targetSymbol.classKind == KtClassKind.ANNOTATION_CLASS) {
                targetSymbol.getMemberScope().getConstructors().firstOrNull() ?: targetSymbol
            } else targetSymbol
        }
    }

    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val results = FirReferenceResolveHelper.resolveSimpleNameReference(this@KtFirSimpleNameReference, this)
        return fixUpAnnotationCallResolveToCtor(results)
    }

    override fun doCanBeReferenceTo(candidateTarget: PsiElement): Boolean {
        return true // TODO
    }

    override fun isReferenceToWithoutExtensionChecking(candidateTarget: PsiElement): Boolean {
        return resolve() == candidateTarget //todo
    }

    override fun handleElementRename(newElementName: String): PsiElement? {
        TODO("Not yet implemented")
    }

    override fun bindToElement(element: PsiElement, shorteningMode: ShorteningMode): PsiElement {
        TODO("Not yet implemented")
    }

    override fun bindToFqName(fqName: FqName, shorteningMode: ShorteningMode, targetElement: PsiElement?): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getImportAlias(): KtImportAlias? {
        TODO("Not yet implemented")
    }

    override val resolver get() = KtFirReferenceResolver
}