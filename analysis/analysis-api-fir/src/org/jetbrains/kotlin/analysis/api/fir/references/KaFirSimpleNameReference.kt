/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.impl.base.references.KaBaseSimpleNameReference
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.expressions.FirLoopJump
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.*

internal class KaFirSimpleNameReference(
    expression: KtSimpleNameExpression,
    val isRead: Boolean,
) : KaBaseSimpleNameReference(expression), KaFirReference {
    private val isAnnotationCall: Boolean
        get() {
            val ktUserType = expression.parent as? KtUserType ?: return false
            val ktTypeReference = ktUserType.parent as? KtTypeReference ?: return false
            val ktConstructorCalleeExpression = ktTypeReference.parent as? KtConstructorCalleeExpression ?: return false
            return ktConstructorCalleeExpression.parent is KtAnnotationEntry
        }

    private fun KaSession.fixUpAnnotationCallResolveToCtor(resultsToFix: Collection<KaSymbol>): Collection<KaSymbol> {
        if (resultsToFix.isEmpty() || !isAnnotationCall) return resultsToFix

        return resultsToFix.map { targetSymbol ->
            if (targetSymbol is KaFirNamedClassSymbol && targetSymbol.classKind == KaClassKind.ANNOTATION_CLASS) {
                targetSymbol.memberScope.constructors.firstOrNull() ?: targetSymbol
            } else targetSymbol
        }
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }

    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        val results = FirReferenceResolveHelper.resolveSimpleNameReference(this@KaFirSimpleNameReference, this)
        //This fix-up needed to resolve annotation call into annotation constructor (but not into the annotation type)
        return fixUpAnnotationCallResolveToCtor(results)
    }

    override fun getResolvedToPsi(analysisSession: KaSession): Collection<PsiElement> = with(analysisSession) {
        if (expression is KtLabelReferenceExpression) {
            val fir = expression.getOrBuildFir((analysisSession as KaFirSession).resolutionFacade)
            if (fir is FirLoopJump) {
                return listOfNotNull(fir.target.labeledElement.psi)
            }
        }
        val referenceTargetSymbols = resolveToSymbols()
        val psiOfReferenceTarget = super.getResolvedToPsi(analysisSession, referenceTargetSymbols)
        if (psiOfReferenceTarget.isNotEmpty()) return psiOfReferenceTarget
        referenceTargetSymbols.flatMap { symbol ->
            when (symbol) {
                is KaFirSyntheticJavaPropertySymbol ->
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
        val name = element.getReferencedName()
        val file = element.containingKtFile
        return getImportAlias(file.findImportByAlias(name))
    }
}
