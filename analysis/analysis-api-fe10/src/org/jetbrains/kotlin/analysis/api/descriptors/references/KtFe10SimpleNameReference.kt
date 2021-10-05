/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.KtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

internal class KtFe10SimpleNameReference(expression: KtSimpleNameExpression) : KtSimpleNameReference(expression), KtFe10Reference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        require(this is KtFe10AnalysisSession)

        val bindingContext = analyze(expression, KtFe10AnalysisSession.AnalysisMode.PARTIAL)

        val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, expression]
            ?: expression.getResolvedCall(bindingContext)?.resultingDescriptor

        return listOfNotNull(descriptor?.toKtSymbol(this))
    }

    override fun doCanBeReferenceTo(candidateTarget: PsiElement): Boolean {
        return true // TODO
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return resolve() == element // TODO
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
}