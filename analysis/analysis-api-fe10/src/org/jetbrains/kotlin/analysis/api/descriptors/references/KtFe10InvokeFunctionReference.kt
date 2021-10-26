/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.CliKtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.KtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

abstract class KtFe10InvokeFunctionReference(expression: KtCallExpression) : KtInvokeFunctionReference(expression), KtFe10Reference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        require(this is KtFe10AnalysisSession)

        val bindingContext = analyze(expression, AnalysisMode.PARTIAL)
        val descriptor = expression.getResolvedCall(bindingContext)?.resultingDescriptor
        return listOfNotNull(descriptor?.toKtCallableSymbol(this))
    }
}

internal class CliKtFe10InvokeFunctionReference(
    expression: KtCallExpression
) : KtFe10InvokeFunctionReference(expression), CliKtFe10Reference {
    override fun doRenameImplicitConventionalCall(newName: String?): KtExpression {
        throw NotImplementedError("Renaming is not supported")
    }
}