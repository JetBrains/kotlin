/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.components.KtImplicitReceiverSmartCastKind
import org.jetbrains.kotlin.analysis.api.components.KtSmartCastInfo
import org.jetbrains.kotlin.analysis.api.components.KtSmartCastProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isStableSmartcast
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis

internal class KtFirSmartcastProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSmartCastProvider(), KtFirAnalysisSessionComponent {

    private val KtExpression.isExplicitSmartCastInfoTarget: Boolean
        get() {
            // we want to handle only most top-level parenthesised expressions
            if (parent is KtParenthesizedExpression) return false

            // expressions like `|foo.bar()|` or `|foo?.baz()|` are ignored
            if (this is KtQualifiedExpression && selectorExpression is KtCallExpression) return false

            // expressions like `foo.|bar|` or `foo?.|baz|` are ignored
            if (this is KtNameReferenceExpression && getQualifiedExpressionForSelector() != null) return false

            // only those types of expressions are supported
            return this is KtQualifiedExpression ||
                    this is KtNameReferenceExpression ||
                    this is KtParenthesizedExpression
        }

    private fun getMatchingFirExpressionWithSmartCast(expression: KtExpression): FirExpressionWithSmartcast? {
        if (!expression.isExplicitSmartCastInfoTarget) return null

        val possibleFunctionCall = expression.getPossiblyQualifiedCallExpressionForCallee() ?: expression

        return when (val firExpression = possibleFunctionCall.getOrBuildFir(analysisSession.firResolveState)) {
            is FirExpressionWithSmartcast -> firExpression
            is FirSafeCallExpression -> firExpression.selector as? FirExpressionWithSmartcast
            is FirImplicitInvokeCall -> firExpression.explicitReceiver as? FirExpressionWithSmartcast
            else -> null
        }
    }

    override fun getSmartCastedInfo(expression: KtExpression): KtSmartCastInfo? = withValidityAssertion {
        val firSmartCastExpression = getMatchingFirExpressionWithSmartCast(expression) ?: return null
        getSmartCastedInfo(firSmartCastExpression)
    }

    private fun getSmartCastedInfo(expression: FirExpressionWithSmartcast): KtSmartCastInfo? {
        val type = expression.smartcastType.coneTypeSafe<ConeKotlinType>()?.asKtType() ?: return null
        return KtSmartCastInfo(type, expression.isStable, token)
    }

    private val KtExpression.isImplicitSmartCastInfoTarget: Boolean
        get() = this is KtNameReferenceExpression || this is KtOperationReferenceExpression

    private fun getMatchingFirQualifiedAccessExpression(expression: KtExpression): FirQualifiedAccessExpression? {
        if (!expression.isImplicitSmartCastInfoTarget) return null

        val wholeExpression = expression.getOperationExpressionForOperationReference()
            ?: expression.getPossiblyQualifiedCallExpressionForCallee()
            ?: expression.getQualifiedExpressionForSelector()
            ?: expression

        return when (val firExpression = wholeExpression.getOrBuildFir(analysisSession.firResolveState)) {
            is FirQualifiedAccessExpression -> firExpression
            is FirSafeCallExpression -> firExpression.selector as? FirQualifiedAccessExpression
            else -> null
        }
    }

    override fun getImplicitReceiverSmartCast(expression: KtExpression): Collection<KtImplicitReceiverSmartCast> = withValidityAssertion {
        val firQualifiedExpression = getMatchingFirQualifiedAccessExpression(expression) ?: return emptyList()

        listOfNotNull(
            smartCastedImplicitReceiver(firQualifiedExpression, KtImplicitReceiverSmartCastKind.DISPATCH),
            smartCastedImplicitReceiver(firQualifiedExpression, KtImplicitReceiverSmartCastKind.EXTENSION),
        )
    }

    private fun smartCastedImplicitReceiver(
        firExpression: FirQualifiedAccessExpression,
        kind: KtImplicitReceiverSmartCastKind,
    ): KtImplicitReceiverSmartCast? {
        val receiver = when (kind) {
            KtImplicitReceiverSmartCastKind.DISPATCH -> firExpression.dispatchReceiver
            KtImplicitReceiverSmartCastKind.EXTENSION -> firExpression.extensionReceiver
        }

        if (receiver == firExpression.explicitReceiver) return null
        if (!receiver.isStableSmartcast()) return null

        val type = receiver.typeRef.coneTypeSafe<ConeKotlinType>()?.asKtType() ?: return null
        return KtImplicitReceiverSmartCast(type, kind, token)
    }
}

private fun KtExpression.getPossiblyQualifiedCallExpressionForCallee(): KtExpression? {
    val expressionParent = this.parent

    return if (expressionParent is KtCallExpression && expressionParent.calleeExpression == this) {
        expressionParent.getQualifiedExpressionForSelectorOrThis()
    } else {
        null
    }
}

private fun KtExpression.getOperationExpressionForOperationReference(): KtOperationExpression? =
    (this as? KtOperationReferenceExpression)?.parent as? KtOperationExpression
