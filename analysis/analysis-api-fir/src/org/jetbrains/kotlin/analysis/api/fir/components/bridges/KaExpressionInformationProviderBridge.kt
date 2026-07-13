/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.KaExpressionInformationProvider
import org.jetbrains.kotlin.analysis.api.components.KaWhenMissingCase
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsExpressionInformationProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.analysis.api.expressions.isStableForSmartCasting as isStableForSmartCastingEndpoint
import org.jetbrains.kotlin.analysis.api.expressions.isUsedAsExpression as isUsedAsExpressionEndpoint
import org.jetbrains.kotlin.analysis.api.expressions.isUsedAsResultOfLambda as isUsedAsResultOfLambdaEndpoint

/**
 * Routes the legacy [KaExpressionInformationProvider] surface through the new public `context(session: KaSession)` expression-information
 * endpoints, which in turn reach the [KaInternalsExpressionInformationProvider] proxy. Members without a public endpoint (the deprecated
 * [KaExpressionInformationProvider.targetSymbol] and the `@KaIdeApi` [KaExpressionInformationProvider.computeMissingCases]) are forwarded
 * straight to the proxy.
 */
internal class KaExpressionInformationProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaExpressionInformationProvider {
    private val proxy: KaInternalsExpressionInformationProvider
        get() = analysisSession.expressionInformationProvider

    @Suppress("OVERRIDE_DEPRECATION")
    override val KtReturnExpression.targetSymbol: KaCallableSymbol?
        get() = proxy.targetSymbol(this)

    override fun KtWhenExpression.computeMissingCases(): List<KaWhenMissingCase> =
        proxy.computeMissingCases(this)

    override val KtExpression.isUsedAsExpression: Boolean
        get() = context(analysisSession) { isUsedAsExpressionEndpoint }

    override val KtExpression.isUsedAsResultOfLambda: Boolean
        get() = context(analysisSession) { isUsedAsResultOfLambdaEndpoint }

    override val KtExpression.isStableForSmartCasting: Boolean
        get() = context(analysisSession) { isStableForSmartCastingEndpoint }
}
