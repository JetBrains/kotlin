/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaIdeApi::class)

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaWhenMissingCase
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsExpressionInformationProvider {
    public fun targetSymbol(returnExpression: KtReturnExpression): KaCallableSymbol?

    public fun computeMissingCases(whenExpression: KtWhenExpression): List<KaWhenMissingCase>

    public fun isUsedAsExpression(expression: KtExpression): Boolean

    public fun isUsedAsResultOfLambda(expression: KtExpression): Boolean

    public fun isStableForSmartCasting(expression: KtExpression): Boolean
}
