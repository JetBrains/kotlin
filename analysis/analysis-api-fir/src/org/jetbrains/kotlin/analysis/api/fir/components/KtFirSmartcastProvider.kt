/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.ImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.ImplicitReceiverSmartcastKind
import org.jetbrains.kotlin.analysis.api.components.KtSmartCastProvider
import org.jetbrains.kotlin.analysis.api.components.SmartCastInfo
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isStableSmartcast
import org.jetbrains.kotlin.psi.KtExpression

internal class KtFirSmartcastProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSmartCastProvider(), KtFirAnalysisSessionComponent {
    override fun getSmartCastedInfo(expression: KtExpression): SmartCastInfo? = withValidityAssertion {
        val smartCastExpression =
            expression.getOrBuildFirSafe<FirExpressionWithSmartcast>(analysisSession.firResolveState) ?: return@withValidityAssertion null
        SmartCastInfo(
            smartCastExpression.smartcastType.coneTypeSafe<ConeKotlinType>()?.asKtType() ?: return@withValidityAssertion null,
            smartCastExpression.isStable
        )

    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getImplicitReceiverSmartCast(expression: KtExpression): Collection<ImplicitReceiverSmartCast> = withValidityAssertion {
        val qualifiedExpression =
            expression.getOrBuildFirSafe<FirQualifiedAccessExpression>(analysisSession.firResolveState) ?: return emptyList()
        val dispatchReceiver = qualifiedExpression.dispatchReceiver
        val extensionReceiver = qualifiedExpression.extensionReceiver
        if ((dispatchReceiver !is FirExpressionWithSmartcast || !dispatchReceiver.isStable) &&
            (extensionReceiver !is FirExpressionWithSmartcast || !extensionReceiver.isStable)
        ) return emptyList()
        buildList {
            dispatchReceiver.takeIf { it.isStableSmartcast() }?.let { smartCasted ->
                ImplicitReceiverSmartCast(
                    smartCasted.typeRef.coneTypeSafe<ConeKotlinType>()?.asKtType() ?: return@let null,
                    ImplicitReceiverSmartcastKind.DISPATCH
                )
            }?.let(::add)
            extensionReceiver.takeIf { it.isStableSmartcast() }?.let { smartCasted ->
                ImplicitReceiverSmartCast(
                    smartCasted.typeRef.coneTypeSafe<ConeKotlinType>()?.asKtType() ?: return@let null,
                    ImplicitReceiverSmartcastKind.EXTENSION
                )
            }?.let(::add)
        }
    }
}