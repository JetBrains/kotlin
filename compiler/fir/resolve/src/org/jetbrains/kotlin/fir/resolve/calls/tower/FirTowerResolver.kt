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
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext
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
        context: ResolutionContext,
        externalCollector: CandidateCollector? = null
    ): CandidateCollector {
        return runResolver(info, context, externalCollector ?: collector, manager)
    }

    fun runResolver(
        info: CallInfo,
        context: ResolutionContext,
        collector: CandidateCollector,
        manager: TowerResolveManager
    ): CandidateCollector {
        val candidateFactoriesAndCollectors = buildCandidateFactoriesAndCollectors(info, collector, context)

        enqueueResolutionTasks(context, manager, candidateFactoriesAndCollectors, info)

        manager.runTasks()
        return collector
    }

    private fun enqueueResolutionTasks(
        context: ResolutionContext,
        manager: TowerResolveManager,
        candidateFactoriesAndCollectors: CandidateFactoriesAndCollectors,
        info: CallInfo
    ) {
        val invokeResolveTowerExtension = FirInvokeResolveTowerExtension(context, manager, candidateFactoriesAndCollectors)

        val mainTask = FirTowerResolveTask(
            components,
            manager,
            TowerDataElementsForName(info.name, components.towerDataContext),
            candidateFactoriesAndCollectors.resultCollector,
            candidateFactoriesAndCollectors.candidateFactory,
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
                    if (receiver.calleeReference is FirSuperReference) {
                        manager.enqueueResolverTask { mainTask.runResolverForSuperReceiver(info, receiver) }
                        invokeResolveTowerExtension.enqueueResolveTasksForSuperReceiver(info, receiver)
                        return
                    }
                }
                if (info.isImplicitInvoke) {
                    invokeResolveTowerExtension.enqueueResolveTasksForImplicitInvokeCall(info, receiver)
                    return
                }
                manager.enqueueResolverTask { mainTask.runResolverForExpressionReceiver(info, receiver) }
                invokeResolveTowerExtension.enqueueResolveTasksForExpressionReceiver(info, receiver)
            }
        }
    }

    fun runResolverForDelegatingConstructor(
        info: CallInfo,
        constructedType: ConeClassLikeType,
        context: ResolutionContext
    ): CandidateCollector {
        val outerType = components.outerClassManager.outerType(constructedType)
        val scope = constructedType.scope(components.session, components.scopeSession, FakeOverrideTypeCalculator.DoNothing) ?: return collector

        val dispatchReceiver =
            if (outerType != null)
                components.implicitReceiverStack.receiversAsReversed().drop(1).firstOrNull {
                    AbstractTypeChecker.isSubtypeOf(components.session.typeContext, it.type, outerType)
                } ?: return collector // TODO: report diagnostic about not-found receiver
            else
                null

        val candidateFactory = CandidateFactory(context, info)
        val resultCollector = collector

        scope.processDeclaredConstructors {
            resultCollector.consumeCandidate(
                TowerGroup.Member,
                candidateFactory.createCandidate(
                    info,
                    it,
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                    scope,
                    dispatchReceiver,
                    extensionReceiverValue = null,
                    builtInExtensionFunctionReceiverValue = null
                ),
                context
            )
        }

        return collector
    }

    private fun buildCandidateFactoriesAndCollectors(
        info: CallInfo,
        collector: CandidateCollector,
        context: ResolutionContext
    ): CandidateFactoriesAndCollectors {
        val candidateFactory = CandidateFactory(context, info)

        return CandidateFactoriesAndCollectors(
            candidateFactory,
            collector
        )
    }

    fun reset() {
        collector.newDataSet()
        manager.reset()
    }
}
