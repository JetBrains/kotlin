/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.components.KaImplicitReceiverSmartCastKind
import org.jetbrains.kotlin.analysis.api.components.KaSmartCastInfo
import org.jetbrains.kotlin.analysis.api.components.KaSmartCastProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isStableSmartcast
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis

internal class KaFirSmartcastProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaSmartCastProvider(), KaFirSessionComponent {

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

    private fun getMatchingFirExpressionWithSmartCast(expression: KtExpression): FirSmartCastExpression? {
        if (!expression.isExplicitSmartCastInfoTarget) return null

        val possibleFunctionCall = expression.getPossiblyQualifiedCallExpressionForCallee() ?: expression

        return when (val firExpression = possibleFunctionCall.getOrBuildFir(analysisSession.firResolveSession)) {
            is FirSmartCastExpression -> firExpression
            is FirSafeCallExpression -> firExpression.selector as? FirSmartCastExpression
            is FirImplicitInvokeCall -> firExpression.explicitReceiver as? FirSmartCastExpression
            else -> null
        }
    }

    override fun getSmartCastedInfo(expression: KtExpression): KaSmartCastInfo? {
        val firSmartCastExpression = getMatchingFirExpressionWithSmartCast(expression) ?: return null
        return getSmartCastedInfo(firSmartCastExpression)
    }

    private fun getSmartCastedInfo(expression: FirSmartCastExpression): KaSmartCastInfo? {
        val type = expression.smartcastType.coneTypeSafe<ConeKotlinType>()?.asKtType() ?: return null
        return KaSmartCastInfo(type, expression.isStable, token)
    }

    private val KtExpression.isImplicitSmartCastInfoTarget: Boolean
        get() = this is KtNameReferenceExpression || this is KtOperationReferenceExpression

    private fun getMatchingFirQualifiedAccessExpression(expression: KtExpression): FirQualifiedAccessExpression? {
        if (!expression.isImplicitSmartCastInfoTarget) return null

        val wholeExpression = expression.getOperationExpressionForOperationReference()
            ?: expression.getPossiblyQualifiedCallExpressionForCallee()
            ?: expression.getQualifiedExpressionForSelector()
            ?: expression

        return when (val firExpression = wholeExpression.getOrBuildFir(analysisSession.firResolveSession)) {
            is FirQualifiedAccessExpression -> firExpression
            is FirSafeCallExpression -> firExpression.selector as? FirQualifiedAccessExpression
            is FirSmartCastExpression -> firExpression.originalExpression as? FirQualifiedAccessExpression
            else -> null
        }
    }

    override fun getImplicitReceiverSmartCast(expression: KtExpression): Collection<KaImplicitReceiverSmartCast> {
        val firQualifiedExpression = getMatchingFirQualifiedAccessExpression(expression) ?: return emptyList()

        return listOfNotNull(
            smartCastedImplicitReceiver(firQualifiedExpression, KaImplicitReceiverSmartCastKind.DISPATCH),
            smartCastedImplicitReceiver(firQualifiedExpression, KaImplicitReceiverSmartCastKind.EXTENSION),
        )
    }

    private fun smartCastedImplicitReceiver(
        firExpression: FirQualifiedAccessExpression,
        kind: KaImplicitReceiverSmartCastKind,
    ): KaImplicitReceiverSmartCast? {
        val receiver = when (kind) {
            KaImplicitReceiverSmartCastKind.DISPATCH -> firExpression.dispatchReceiver
            KaImplicitReceiverSmartCastKind.EXTENSION -> firExpression.extensionReceiver
        }

        if (receiver == null || receiver == firExpression.explicitReceiver) return null
        if (!receiver.isStableSmartcast()) return null

        val type = receiver.resolvedType.asKtType()
        return KaImplicitReceiverSmartCast(type, kind, token)
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
