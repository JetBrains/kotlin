/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

abstract class KtExpressionInfoProvider : KtAnalysisSessionComponent() {
    abstract fun getReturnExpressionTargetSymbol(returnExpression: KtReturnExpression): KtCallableSymbol?
    abstract fun getWhenMissingCases(whenExpression: KtWhenExpression): List<WhenMissingCase>
}

interface KtExpressionInfoProviderMixIn : KtAnalysisSessionMixIn {
    fun KtReturnExpression.getReturnTargetSymbol(): KtCallableSymbol? =
        analysisSession.expressionInfoProvider.getReturnExpressionTargetSymbol(this)

    fun KtWhenExpression.getMissingCases(): List<WhenMissingCase> = analysisSession.expressionInfoProvider.getWhenMissingCases(this)
}