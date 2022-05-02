/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.KtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

class KtFe10SimpleNameReference(expression: KtSimpleNameExpression) : KtSimpleNameReference(expression), KtFe10Reference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        require(this is KtFe10AnalysisSession)

        val bindingContext = analysisContext.analyze(expression, AnalysisMode.PARTIAL)

        val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, expression]
            ?: expression.getResolvedCall(bindingContext)?.resultingDescriptor

        return listOfNotNull(descriptor?.toKtSymbol(analysisContext))
    }

    override fun isReferenceToViaExtension(element: PsiElement): Boolean = false

    override fun getImportAlias(): KtImportAlias? {
        return null
    }
}
