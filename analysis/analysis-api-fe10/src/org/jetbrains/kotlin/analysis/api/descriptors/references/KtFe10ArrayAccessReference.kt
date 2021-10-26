/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.CliKtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.KtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtArrayAccessReference
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

abstract class KtFe10ArrayAccessReference(expression: KtArrayAccessExpression) : KtArrayAccessReference(expression), KtFe10Reference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        require(this is KtFe10AnalysisSession)

        val bindingContext = analyze(expression, AnalysisMode.PARTIAL)
        val descriptor = expression.getResolvedCall(bindingContext)?.resultingDescriptor
        return listOfNotNull(descriptor?.toKtCallableSymbol(this))
    }
}

class CliKtFe10ArrayAccessReference(expression: KtArrayAccessExpression) : KtFe10ArrayAccessReference(expression), CliKtFe10Reference {
    override fun handleElementRename(newElementName: String): PsiElement? {
        throw NotImplementedError("Renaming is not supported")
    }
}