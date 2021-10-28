/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtExpressionInfoProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10ExpressionInfoProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtExpressionInfoProvider(), Fe10KtAnalysisSessionComponent {
    override val token: ValidityToken
        get() = analysisSession.token

    override fun getReturnExpressionTargetSymbol(returnExpression: KtReturnExpression): KtCallableSymbol? = withValidityAssertion {
        val bindingContext = analysisContext.analyze(returnExpression, AnalysisMode.PARTIAL)
        val targetLabel = returnExpression.getTargetLabel() ?: return null
        val labelTarget = bindingContext[BindingContext.LABEL_TARGET, targetLabel] as? KtDeclaration ?: return null
        return with(analysisSession) { labelTarget.getSymbol() as? KtCallableSymbol }
    }

    override fun getWhenMissingCases(whenExpression: KtWhenExpression): List<WhenMissingCase> = withValidityAssertion {
        val bindingContext = analysisContext.analyze(whenExpression)
        return WhenChecker.getMissingCases(whenExpression, bindingContext)
    }
}