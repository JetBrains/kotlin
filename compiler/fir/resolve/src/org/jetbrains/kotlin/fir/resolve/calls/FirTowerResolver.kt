/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents

class FirTowerResolver(
    private val components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner,
) {
    private val collector = CandidateCollector(components, resolutionStageRunner)
    private val manager = TowerResolveManager(collector)

    fun runResolver(
        implicitReceiverValues: List<ImplicitReceiverValue<*>>,
        info: CallInfo,
        collector: CandidateCollector = this.collector,
        manager: TowerResolveManager = this.manager
    ): CandidateCollector {
        val candidateFactoriesAndCollectors = buildCandidateFactoriesAndCollectors(info, collector)

        val towerResolverSession = FirTowerResolverSession(components, implicitReceiverValues, manager, candidateFactoriesAndCollectors)
        towerResolverSession.runResolution(info)

        manager.runTasks()
        return collector
    }

    private fun buildCandidateFactoriesAndCollectors(
        info: CallInfo,
        collector: CandidateCollector
    ): CandidateFactoriesAndCollectors {
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

        return CandidateFactoriesAndCollectors(
            candidateFactory,
            collector,
            stubReceiverCandidateFactory,
            invokeReceiverCandidateFactory,
            invokeReceiverCollector,
            invokeBuiltinExtensionReceiverCandidateFactory
        )
    }

    fun reset() {
        collector.newDataSet()
        manager.reset()
    }
}
