/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaExpressionInfoProvider
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

internal class KaFe10ExpressionInfoProvider(
    override val analysisSession: KaFe10Session
) : KaExpressionInfoProvider(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun getReturnExpressionTargetSymbol(returnExpression: KtReturnExpression): KaCallableSymbol? {
        val bindingContext = analysisContext.analyze(returnExpression, AnalysisMode.PARTIAL)
        val targetLabel = returnExpression.getTargetLabel()
            ?: return returnExpression.parentOfType<KtNamedFunction>()
                ?.let { with(analysisSession) { it.getSymbol() as? KaCallableSymbol } }
        val labelTarget = bindingContext[BindingContext.LABEL_TARGET, targetLabel] as? KtDeclaration ?: return null
        return with(analysisSession) { labelTarget.getSymbol() as? KaCallableSymbol }
    }

    override fun getWhenMissingCases(whenExpression: KtWhenExpression): List<WhenMissingCase>  {
        val bindingContext = analysisContext.analyze(whenExpression)
        return WhenChecker.getMissingCases(whenExpression, bindingContext)
    }

    override fun isUsedAsExpression(expression: KtExpression): Boolean {
        val bindingContext = analysisContext.analyze(expression)
        return expression.isUsedAsExpression(bindingContext)
    }
}