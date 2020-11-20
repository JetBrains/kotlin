/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.frontend.api.ImplicitReceiverSmartCast
import org.jetbrains.kotlin.idea.frontend.api.ImplicitReceiverSmartcastKind
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtSmartCastProvider
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression

internal class KtFirTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtTypeProvider(), KtFirAnalysisSessionComponent {
    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType = withValidityAssertion {
        val firDeclaration = declaration.getOrBuildFirOfType<FirCallableDeclaration<*>>(firResolveState)
        firDeclaration.returnTypeRef.coneType.asKtType()
    }

    override fun getKtExpressionType(expression: KtExpression): KtType = withValidityAssertion {
        expression.getOrBuildFirOfType<FirExpression>(firResolveState).typeRef.coneType.asKtType()
    }
}