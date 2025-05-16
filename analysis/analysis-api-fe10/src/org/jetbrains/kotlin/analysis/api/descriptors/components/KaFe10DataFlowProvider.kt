/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSmartCastInfo
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.ExplicitSmartCasts
import org.jetbrains.kotlin.resolve.calls.smartcasts.MultipleSmartCasts
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes

internal class KaFe10DataFlowProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaDataFlowProvider, KaFe10SessionComponent {
    override val KtExpression.smartCastInfo: KaSmartCastInfo?
        get() = withPsiValidityAssertion {
            val bindingContext = analysisContext.analyze(this)
            val stableSmartCasts = bindingContext[BindingContext.SMARTCAST, this]
            val unstableSmartCasts = bindingContext[BindingContext.UNSTABLE_SMARTCAST, this]

            when {
                stableSmartCasts != null -> {
                    val type = stableSmartCasts.asSingleType() ?: return null
                    KaBaseSmartCastInfo(type, true)
                }
                unstableSmartCasts != null -> {
                    val type = unstableSmartCasts.asSingleType() ?: return null
                    KaBaseSmartCastInfo(type, false)
                }
                else -> null
            }
        }

    private fun ExplicitSmartCasts.asSingleType(): KaType? {
        if (this is MultipleSmartCasts) {
            return intersectWrappedTypes(map.values).toKtType(analysisContext)
        }
        return defaultType?.toKtType(analysisContext)
    }

    override val KtExpression.implicitReceiverSmartCasts: Collection<KaImplicitReceiverSmartCast>
        get() = withPsiValidityAssertion {
            val bindingContext = analysisContext.analyze(this)
            val smartCasts = bindingContext[BindingContext.IMPLICIT_RECEIVER_SMARTCAST, this] ?: return emptyList()
            val call = bindingContext[BindingContext.CALL, this] ?: return emptyList()
            val resolvedCall = bindingContext[BindingContext.RESOLVED_CALL, call] ?: return emptyList()

            listOfNotNull(
                createImplicitReceiverSmartCast(
                    smartCasts.receiverTypes[resolvedCall.dispatchReceiver],
                    KaImplicitReceiverSmartCastKind.DISPATCH
                ),
                createImplicitReceiverSmartCast(
                    smartCasts.receiverTypes[resolvedCall.extensionReceiver],
                    KaImplicitReceiverSmartCastKind.EXTENSION
                )
            )
        }

    private fun createImplicitReceiverSmartCast(type: KotlinType?, kind: KaImplicitReceiverSmartCastKind): KaImplicitReceiverSmartCast? {
        if (type == null) return null
        return KaBaseImplicitReceiverSmartCast(type.toKtType(analysisContext), kind)
    }

    override fun computeExitPointSnapshot(
        statements: List<KtExpression>,
    ): KaDataFlowExitPointSnapshot = withPsiValidityAssertion(statements) {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}
