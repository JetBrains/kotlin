/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

internal class KaFe10Resolver(override val analysisSession: KtFe10AnalysisSession) : KaResolver(), Fe10KtAnalysisSessionComponent {
    override fun resolveCallElementToSymbol(callElement: KtCallElement): KtCallableSymbol? {
        val bindingContext = analysisContext.analyze(callElement, AnalysisMode.PARTIAL)
        val resolvedCall = callElement.getResolvedCall(bindingContext) ?: return null
        if (!resolvedCall.status.isSuccess) return null

        return resolvedCall.candidateDescriptor.toKtCallableSymbol(analysisContext)
    }

    override fun resolveReferenceExpressionToSymbol(expression: KtReferenceExpression): KtSymbol? {
        val bindingContext = analysisContext.analyze(expression, AnalysisMode.PARTIAL)
        val resolvedCall = expression.getResolvedCall(bindingContext)
        if (resolvedCall != null) {
            return resolvedCall.takeIf { it.status.isSuccess }?.candidateDescriptor?.toKtSymbol(analysisContext)
        }

        return null
    }
}
