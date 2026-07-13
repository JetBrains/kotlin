/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.analysis.api.dataflow.KaDataFlowExitPointSnapshot as KaEndpointDataFlowExitPointSnapshot
import org.jetbrains.kotlin.analysis.api.dataflow.KaImplicitReceiverSmartCast as KaEndpointImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.dataflow.KaImplicitReceiverSmartCastKind as KaEndpointImplicitReceiverSmartCastKind
import org.jetbrains.kotlin.analysis.api.dataflow.computeExitPointSnapshot as computeExitPointSnapshotEndpoint
import org.jetbrains.kotlin.analysis.api.dataflow.implicitReceiverSmartCasts as implicitReceiverSmartCastsEndpoint
import org.jetbrains.kotlin.analysis.api.dataflow.smartCastInfo as smartCastInfoEndpoint

internal class KaDataFlowProviderBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaDataFlowProvider {
    override val KtExpression.smartCastInfo: KaSmartCastInfo?
        get() = context(analysisSession) { smartCastInfoEndpoint as KaSmartCastInfo? }

    override val KtExpression.implicitReceiverSmartCasts: Collection<KaImplicitReceiverSmartCast>
        get() = context(analysisSession) { implicitReceiverSmartCastsEndpoint.map { it.toOld() } }

    override fun computeExitPointSnapshot(statements: List<KtExpression>): KaDataFlowExitPointSnapshot =
        context(analysisSession) { computeExitPointSnapshotEndpoint(statements).toOld() }
}

private fun KaEndpointImplicitReceiverSmartCast.toOld(): KaImplicitReceiverSmartCast {
    val endpoint = this
    return object : KaImplicitReceiverSmartCast {
        override val token: KaLifetimeToken get() = endpoint.token
        override val type: KaType get() = endpoint.type
        override val kind: KaImplicitReceiverSmartCastKind get() = endpoint.kind.toOld()
    }
}

private fun KaEndpointImplicitReceiverSmartCastKind.toOld(): KaImplicitReceiverSmartCastKind = when (this) {
    KaEndpointImplicitReceiverSmartCastKind.DISPATCH -> KaImplicitReceiverSmartCastKind.DISPATCH
    KaEndpointImplicitReceiverSmartCastKind.EXTENSION -> KaImplicitReceiverSmartCastKind.EXTENSION
}

private fun KaEndpointDataFlowExitPointSnapshot.toOld(): KaDataFlowExitPointSnapshot = KaDataFlowExitPointSnapshot(
    defaultExpressionInfo = defaultExpressionInfo?.let {
        KaDataFlowExitPointSnapshot.DefaultExpressionInfo(it.expression, it.type)
    },
    valuedReturnExpressions = valuedReturnExpressions,
    returnValueType = returnValueType,
    jumpExpressions = jumpExpressions,
    hasJumps = hasJumps,
    hasEscapingJumps = hasEscapingJumps,
    hasMultipleJumpKinds = hasMultipleJumpKinds,
    hasMultipleJumpTargets = hasMultipleJumpTargets,
    variableReassignments = variableReassignments.map {
        KaDataFlowExitPointSnapshot.VariableReassignment(it.expression, it.variable, it.isAugmented)
    },
)
