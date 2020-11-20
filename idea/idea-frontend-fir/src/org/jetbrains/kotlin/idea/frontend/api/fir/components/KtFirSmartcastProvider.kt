/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.ImplicitReceiverSmartCast
import org.jetbrains.kotlin.idea.frontend.api.ImplicitReceiverSmartcastKind
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtSmartCastProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtExpression

internal class KtFirSmartcastProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
    ) : KtSmartCastProvider(), KtFirAnalysisSessionComponent {
    override fun getSmartCastedToTypes(expression: KtExpression): Collection<KtType> = withValidityAssertion {
        // TODO filter out not used smartcasts
        expression.getOrBuildFirSafe<FirExpressionWithSmartcast>(analysisSession.firResolveState)
            ?.typesFromSmartCast
            ?.map { it.asKtType() }
            ?: emptyList()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getImplicitReceiverSmartCasts(expression: KtExpression): Collection<ImplicitReceiverSmartCast> = withValidityAssertion {
        // TODO filter out not used smartcasts
        val qualifiedExpression =
            expression.getOrBuildFirSafe<FirQualifiedAccessExpression>(analysisSession.firResolveState) ?: return emptyList()
        if (qualifiedExpression.dispatchReceiver !is FirExpressionWithSmartcast
            && qualifiedExpression.extensionReceiver !is FirExpressionWithSmartcast
        ) return emptyList()
        buildList {
            (qualifiedExpression.dispatchReceiver as? FirExpressionWithSmartcast)?.let { smartCasted ->
                ImplicitReceiverSmartCast(
                    smartCasted.typesFromSmartCast.map { it.asKtType() },
                    ImplicitReceiverSmartcastKind.DISPATCH
                )
            }?.let(::add)
            (qualifiedExpression.extensionReceiver as? FirExpressionWithSmartcast)?.let { smartCasted ->
                ImplicitReceiverSmartCast(
                    smartCasted.typesFromSmartCast.map { it.asKtType() },
                    ImplicitReceiverSmartcastKind.EXTENSION
                )
            }?.let(::add)
        }
    }
}