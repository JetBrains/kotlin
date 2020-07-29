/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.AbstractTypeChecker

class FirTowerResolver(
    private val components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner,
) {
    private val collector = CandidateCollector(components, resolutionStageRunner)
    private val manager = TowerResolveManager(collector)

    fun runResolver(
        info: CallInfo,
        collector: CandidateCollector = this.collector,
        manager: TowerResolveManager = this.manager
    ): CandidateCollector {
        val candidateFactoriesAndCollectors = buildCandidateFactoriesAndCollectors(info, collector)

        enqueueResolutionTasks(components, manager, candidateFactoriesAndCollectors, info)

        manager.runTasks()
        return collector
    }

    private fun enqueueResolutionTasks(
        components: BodyResolveComponents,
        manager: TowerResolveManager,
        candidateFactoriesAndCollectors: CandidateFactoriesAndCollectors,
        info: CallInfo
    ) {
        val invokeResolveTowerExtension = FirInvokeResolveTowerExtension(components, manager, candidateFactoriesAndCollectors)

        val mainTask = FirTowerResolveTask(
            components,
            manager,
            TowerDataElementsForName(info.name, components.towerDataContext),
            candidateFactoriesAndCollectors.resultCollector,
            candidateFactoriesAndCollectors.candidateFactory,
            candidateFactoriesAndCollectors.stubReceiverCandidateFactory
        )
        when (val receiver = info.explicitReceiver) {
            is FirResolvedQualifier -> {
                manager.enqueueResolverTask { mainTask.runResolverForQualifierReceiver(info, receiver) }
                invokeResolveTowerExtension.enqueueResolveTasksForQualifier(info, receiver)
            }
            null -> {
                manager.enqueueResolverTask { mainTask.runResolverForNoReceiver(info) }
                invokeResolveTowerExtension.enqueueResolveTasksForNoReceiver(info)
            }
            else -> {
                if (receiver is FirQualifiedAccessExpression) {
                    val calleeReference = receiver.calleeReference
                    if (calleeReference is FirSuperReference) {
                        return manager.enqueueResolverTask { mainTask.runResolverForSuperReceiver(info, receiver.typeRef) }
                    }
                }

                manager.enqueueResolverTask { mainTask.runResolverForExpressionReceiver(info, receiver) }
                invokeResolveTowerExtension.enqueueResolveTasksForExpressionReceiver(info, receiver)
            }
        }
    }

    fun runResolverForDelegatingConstructor(
        info: CallInfo,
        constructedType: ConeClassLikeType
    ): CandidateCollector {
        val outerType = components.outerClassManager.outerType(constructedType)
        val scope = constructedType.scope(components.session, components.scopeSession) ?: return collector

        val dispatchReceiver =
            if (outerType != null)
                components.implicitReceiverStack.receiversAsReversed().drop(1).firstOrNull {
                    AbstractTypeChecker.isSubtypeOf(components.session.typeContext, it.type, outerType)
                } ?: return collector // TODO: report diagnostic about not-found receiver
            else
                null

        val candidateFactory = CandidateFactory(components, info)
        val resultCollector = collector

        scope.processDeclaredConstructors {
            resultCollector.consumeCandidate(
                TowerGroup.Member,
                candidateFactory.createCandidate(
                    it,
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                    dispatchReceiver,
                    implicitExtensionReceiverValue = null,
                    builtInExtensionFunctionReceiverValue = null
                )
            )
        }

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

        return CandidateFactoriesAndCollectors(
            candidateFactory,
            collector,
            stubReceiverCandidateFactory
        )
    }

    fun reset() {
        collector.newDataSet()
        manager.reset()
    }
}
