/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.components.KaImplicitReceiverSmartCastKind
import org.jetbrains.kotlin.analysis.api.components.KaSmartCastInfo
import org.jetbrains.kotlin.analysis.api.components.KaSmartCastProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.ExplicitSmartCasts
import org.jetbrains.kotlin.resolve.calls.smartcasts.MultipleSmartCasts
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes

internal class KaFe10SmartCastProvider(
    override val analysisSession: KaFe10Session
) : KaSmartCastProvider(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun getSmartCastedInfo(expression: KtExpression): KaSmartCastInfo? {
        val bindingContext = analysisContext.analyze(expression)
        val stableSmartCasts = bindingContext[BindingContext.SMARTCAST, expression]
        val unstableSmartCasts = bindingContext[BindingContext.UNSTABLE_SMARTCAST, expression]

        return when {
            stableSmartCasts != null -> {
                val type = stableSmartCasts.getKtType() ?: return null
                KaSmartCastInfo(type, true, token)
            }
            unstableSmartCasts != null -> {
                val type = unstableSmartCasts.getKtType() ?: return null
                KaSmartCastInfo(type, false, token)
            }
            else -> null
        }
    }

    private fun ExplicitSmartCasts.getKtType(): KaType? {
        if (this is MultipleSmartCasts) {
            return intersectWrappedTypes(map.values).toKtType(analysisContext)
        }
        return defaultType?.toKtType(analysisContext)
    }

    private fun smartCastedImplicitReceiver(type: KotlinType?, kind: KaImplicitReceiverSmartCastKind): KaImplicitReceiverSmartCast? {
        if (type == null) return null
        return KaImplicitReceiverSmartCast(type.toKtType(analysisContext), kind, token)
    }

    override fun getImplicitReceiverSmartCast(expression: KtExpression): Collection<KaImplicitReceiverSmartCast> {
        val bindingContext = analysisContext.analyze(expression)
        val smartCasts = bindingContext[BindingContext.IMPLICIT_RECEIVER_SMARTCAST, expression] ?: return emptyList()
        val call = bindingContext[BindingContext.CALL, expression] ?: return emptyList()
        val resolvedCall = bindingContext[BindingContext.RESOLVED_CALL, call] ?: return emptyList()
        return listOfNotNull(
            smartCastedImplicitReceiver(smartCasts.receiverTypes[resolvedCall.dispatchReceiver], KaImplicitReceiverSmartCastKind.DISPATCH),
            smartCastedImplicitReceiver(smartCasts.receiverTypes[resolvedCall.extensionReceiver], KaImplicitReceiverSmartCastKind.EXTENSION)
        )
    }
}