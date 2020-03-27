/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator

class FirTowerResolver(
    val typeCalculator: ReturnTypeCalculator,
    val components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner,
) {
    private val collector = CandidateCollector(components, resolutionStageRunner)
    private val manager = TowerResolveManager()

    fun runResolver(
        implicitReceiverValues: List<ImplicitReceiverValue<*>>,
        info: CallInfo,
        collector: CandidateCollector = this.collector,
        manager: TowerResolveManager = this.manager
    ): CandidateCollector {
        val towerResolverSession = FirTowerResolverSession(components, implicitReceiverValues, manager)
        manager.callResolutionContext = buildCallResolutionContext(info, collector, towerResolverSession)

        towerResolverSession.runResolution(info)

        manager.runTasks()
        return collector
    }

    private fun buildCallResolutionContext(
        info: CallInfo,
        collector: CandidateCollector,
        towerResolverSession: FirTowerResolverSession
    ): CallResolutionContext {
        val candidateFactory = CandidateFactory(components, info)
        val stubReceiverCandidateFactory =
            if (info.callKind == CallKind.CallableReference && info.stubReceiver != null)
                candidateFactory.replaceCallInfo(info.replaceExplicitReceiver(info.stubReceiver))
            else
                null

        var invokeReceiverCollector: CandidateCollector? = null
        var invokeReceiverCandidateFactory: CandidateFactory? = null
        var invokeBuiltinExtensionReceiverCandidateFactory: CandidateFactory? = null
        if (info.callKind == CallKind.Function) {
            invokeReceiverCollector = CandidateCollector(components, components.resolutionStageRunner)
            invokeReceiverCandidateFactory = CandidateFactory(components, info.replaceWithVariableAccess())
            if (info.explicitReceiver != null) {
                with(invokeReceiverCandidateFactory) {
                    invokeBuiltinExtensionReceiverCandidateFactory = replaceCallInfo(callInfo.replaceExplicitReceiver(null))
                }
            }
        }

        return CallResolutionContext(
            candidateFactory,
            collector,
            invokeReceiverCandidateFactory,
            invokeBuiltinExtensionReceiverCandidateFactory,
            stubReceiverCandidateFactory,
            invokeReceiverCollector,
            this,
            towerResolverSession
        )
    }

    fun reset() {
        collector.newDataSet()
        manager.reset()
    }
}
