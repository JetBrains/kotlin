/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.*

internal class KtFirSimpleNameReference(
    expression: KtSimpleNameExpression,
    val isRead: Boolean,
) : KtSimpleNameReference(expression), KtFirReference {

    private val isAnnotationCall: Boolean
        get() {
            val ktUserType = expression.parent as? KtUserType ?: return false
            val ktTypeReference = ktUserType.parent as? KtTypeReference ?: return false
            val ktConstructorCalleeExpression = ktTypeReference.parent as? KtConstructorCalleeExpression ?: return false
            return ktConstructorCalleeExpression.parent is KtAnnotationEntry
        }

    private fun KtAnalysisSession.fixUpAnnotationCallResolveToCtor(resultsToFix: Collection<KtSymbol>): Collection<KtSymbol> {
        if (resultsToFix.isEmpty() || !isAnnotationCall) return resultsToFix

        return resultsToFix.map { targetSymbol ->
            if (targetSymbol is KtFirNamedClassOrObjectSymbol && targetSymbol.classKind == KtClassKind.ANNOTATION_CLASS) {
                targetSymbol.getMemberScope().getConstructors().firstOrNull() ?: targetSymbol
            } else targetSymbol
        }
    }

    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val results = FirReferenceResolveHelper.resolveSimpleNameReference(this@KtFirSimpleNameReference, this)
        //This fix-up needed to resolve annotation call into annotation constructor (but not into the annotation type)
        return fixUpAnnotationCallResolveToCtor(results)
    }

    override fun getResolvedToPsi(analysisSession: KtAnalysisSession): Collection<PsiElement> = with(analysisSession) {
        val referenceTargetSymbols = resolveToSymbols()
        val psiOfReferenceTarget = super.getResolvedToPsi(analysisSession, referenceTargetSymbols)
        if (psiOfReferenceTarget.isNotEmpty()) return psiOfReferenceTarget
        referenceTargetSymbols.flatMap { symbol ->
            when (symbol) {
                is KtFirSyntheticJavaPropertySymbol ->
                    if (isRead) {
                        listOfNotNull(symbol.javaGetterSymbol.psi)
                    } else {
                        if (symbol.javaSetterSymbol == null) listOfNotNull(symbol.javaGetterSymbol.psi)
                        else listOfNotNull(symbol.javaSetterSymbol?.psi)
                    }
                else -> listOfNotNull(symbol.psi)
            }
        }
    }

    override fun canBeReferenceTo(candidateTarget: PsiElement): Boolean {
        return true // TODO
    }

    // Extension point used for deprecated Android Extensions. Not going to implement for FIR.
    override fun isReferenceToViaExtension(element: PsiElement): Boolean {
        return false
    }

    override fun getImportAlias(): KtImportAlias? {
        // TODO: Implement.
        return null
    }
}