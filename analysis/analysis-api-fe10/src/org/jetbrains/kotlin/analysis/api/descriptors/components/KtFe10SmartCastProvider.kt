/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.components.KtImplicitReceiverSmartCastKind
import org.jetbrains.kotlin.analysis.api.components.KtSmartCastInfo
import org.jetbrains.kotlin.analysis.api.components.KtSmartCastProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.ExplicitSmartCasts
import org.jetbrains.kotlin.resolve.calls.smartcasts.MultipleSmartCasts
import org.jetbrains.kotlin.types.TypeIntersector

internal class KtFe10SmartCastProvider(override val analysisSession: KtFe10AnalysisSession) : KtSmartCastProvider() {
    override val token: ValidityToken
        get() = analysisSession.token

    override fun getSmartCastedInfo(expression: KtExpression): KtSmartCastInfo? {
        withValidityAssertion {
            val bindingContext = analysisSession.analyze(expression)
            val stableSmartCasts = bindingContext[BindingContext.SMARTCAST, expression]
            val unstableSmartCasts = bindingContext[BindingContext.UNSTABLE_SMARTCAST, expression]

            return when {
                stableSmartCasts != null -> {
                    val type = stableSmartCasts.getKtType() ?: return null
                    KtSmartCastInfo(type, true, token)
                }
                unstableSmartCasts != null -> {
                    val type = unstableSmartCasts.getKtType() ?: return null
                    KtSmartCastInfo(type, false, token)
                }
                else -> null
            }
        }
    }

    private fun ExplicitSmartCasts.getKtType(): KtType? {
        if (this is MultipleSmartCasts) {
            return TypeIntersector.intersectTypes(map.values)?.toKtType(analysisSession)
        }
        return defaultType?.toKtType(analysisSession)
    }

    override fun getImplicitReceiverSmartCast(expression: KtExpression): Collection<KtImplicitReceiverSmartCast> {
        withValidityAssertion {
            val bindingContext = analysisSession.analyze(expression)
            val smartCasts = bindingContext[BindingContext.IMPLICIT_RECEIVER_SMARTCAST, expression] ?: return emptyList()
            return smartCasts.receiverTypes.map { (_, type) ->
                val kind = KtImplicitReceiverSmartCastKind.DISPATCH // TODO provide precise kind
                KtImplicitReceiverSmartCast(type.toKtType(analysisSession), kind, token)
            }
        }
    }
}