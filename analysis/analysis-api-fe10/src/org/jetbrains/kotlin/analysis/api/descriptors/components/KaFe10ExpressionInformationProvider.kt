/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaExpressionInformationProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsResultOfLambda

internal class KaFe10ExpressionInformationProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaExpressionInformationProvider, KaFe10SessionComponent {
    @Deprecated("The API is obsolete. Use `resolveSymbol()` instead.", replaceWith = ReplaceWith("resolveSymbol()"))
    override val KtReturnExpression.targetSymbol: KaCallableSymbol?
        get() = with(analysisSession) { resolveSymbol() }

    override fun KtWhenExpression.computeMissingCases(): List<WhenMissingCase> = withPsiValidityAssertion {
        val bindingContext = analysisContext.analyze(this)
        return WhenChecker.getMissingCases(this, bindingContext)
    }

    override val KtExpression.isUsedAsExpression: Boolean
        get() = withPsiValidityAssertion {
            val bindingContext = analysisContext.analyze(this)
            return isUsedAsExpression(bindingContext)
        }
    override val KtExpression.isUsedAsResultOfLambda: Boolean
        get() = withPsiValidityAssertion {
            val bindingContext = analysisContext.analyze(this)
            return isUsedAsResultOfLambda(bindingContext)
        }
}