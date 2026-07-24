/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsExpressionTypeProvider
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtDeclarationWithReturnType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.analysis.api.expressions.expectedType as expectedTypeEndpoint
import org.jetbrains.kotlin.analysis.api.expressions.expressionType as expressionTypeEndpoint
import org.jetbrains.kotlin.analysis.api.expressions.functionType as functionTypeEndpoint
import org.jetbrains.kotlin.analysis.api.expressions.isDefinitelyNotNull as isDefinitelyNotNullEndpoint
import org.jetbrains.kotlin.analysis.api.expressions.isDefinitelyNull as isDefinitelyNullEndpoint

/**
 * Routes the legacy [KaExpressionTypeProvider] surface through the new public `context(session: KaSession)` expression-type endpoints,
 * which in turn reach the [KaInternalsExpressionTypeProvider] proxy. The [KaExpressionTypeProvider.returnType] members have no public
 * endpoint yet (KT-73570), so [KtDeclarationWithReturnType.returnType] is forwarded straight to the proxy and the deprecated
 * [KtDeclaration.returnType][KaExpressionTypeProvider.returnType] keeps the interface default that routes to it.
 */
internal class KaExpressionTypeProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaExpressionTypeProvider {
    private val proxy: KaInternalsExpressionTypeProvider
        get() = analysisSession.expressionTypeProvider

    override val KtExpression.expressionType: KaType?
        get() = context(analysisSession) { expressionTypeEndpoint }

    override val KtDeclarationWithReturnType.returnType: KaType
        get() = proxy.returnType(this)

    override val KtFunction.functionType: KaType
        get() = context(analysisSession) { functionTypeEndpoint }

    override val PsiElement.expectedType: KaType?
        get() = context(analysisSession) { expectedTypeEndpoint }

    override val KtExpression.isDefinitelyNull: Boolean
        get() = context(analysisSession) { isDefinitelyNullEndpoint }

    override val KtExpression.isDefinitelyNotNull: Boolean
        get() = context(analysisSession) { isDefinitelyNotNullEndpoint }
}
