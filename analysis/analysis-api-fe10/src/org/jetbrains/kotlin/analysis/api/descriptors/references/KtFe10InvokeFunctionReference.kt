/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.KtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

class KtFe10InvokeFunctionReference(expression: KtCallExpression) : KtInvokeFunctionReference(expression), KtFe10Reference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        require(this is KtFe10AnalysisSession)

        val bindingContext = analysisContext.analyze(expression, AnalysisMode.PARTIAL)
        val call = expression.getCall(bindingContext)
        val resolvedCall = call.getResolvedCall(bindingContext)
        val descriptors = when {
            resolvedCall is VariableAsFunctionResolvedCall ->
                setOf((resolvedCall as VariableAsFunctionResolvedCall).functionCall.resultingDescriptor)
            call != null && resolvedCall != null && call.callType == Call.CallType.INVOKE ->
                setOf(resolvedCall.resultingDescriptor)
            else ->
                emptyList()
        }
        return descriptors.mapNotNull { it.toKtCallableSymbol(analysisContext) }
    }
}
