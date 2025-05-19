/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaExpressionInformationProvider
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

internal class KaFe10ExpressionInformationProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaExpressionInformationProvider, KaFe10SessionComponent {
    override val KtReturnExpression.targetSymbol: KaCallableSymbol?
        get() = withPsiValidityAssertion {
            val bindingContext = analysisContext.analyze(this, AnalysisMode.PARTIAL)
            val targetLabel = getTargetLabel() ?: return parentOfType<KtNamedFunction>()?.let { with(analysisSession) { it.symbol } }
            val labelTarget = bindingContext[BindingContext.LABEL_TARGET, targetLabel] as? KtDeclaration ?: return null
            return with(analysisSession) { labelTarget.symbol as? KaCallableSymbol }
        }

    override fun KtWhenExpression.computeMissingCases(): List<WhenMissingCase> = withPsiValidityAssertion {
        val bindingContext = analysisContext.analyze(this)
        return WhenChecker.getMissingCases(this, bindingContext)
    }

    override val KtExpression.isUsedAsExpression: Boolean
        get() = withPsiValidityAssertion {
            val bindingContext = analysisContext.analyze(this)
            return isUsedAsExpression(bindingContext)
        }
}