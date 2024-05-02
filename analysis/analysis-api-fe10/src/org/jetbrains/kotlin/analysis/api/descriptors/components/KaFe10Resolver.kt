/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

internal class KaFe10Resolver(override val analysisSession: KtFe10AnalysisSession) : KaResolver(), Fe10KtAnalysisSessionComponent {
    override fun resolveKtElementToSymbol(ktElement: KtElement): KtSymbol? {
        if (ktElement is KtCallableReferenceExpression) {
            return ktElement.callableReference.let(::resolveKtElementToSymbol)
        }

        val bindingContext = analysisContext.analyze(ktElement, AnalysisMode.PARTIAL)
        val resolvedCall = ktElement.getResolvedCall(bindingContext)
        if (resolvedCall != null) {
            return resolvedCall.takeIf { it.status.isSuccess }?.candidateDescriptor?.toKtSymbol(analysisContext)
        }

        if (ktElement is KtReferenceExpression) {
            val labeledDeclaration = bindingContext[BindingContext.LABEL_TARGET, ktElement] ?: return null
            val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, labeledDeclaration] ?: return null
            return descriptor.toKtSymbol(analysisContext)
        }

        return null
    }
}
