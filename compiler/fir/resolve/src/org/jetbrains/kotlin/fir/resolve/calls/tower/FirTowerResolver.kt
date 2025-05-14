/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirSuperReceiverExpression
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.InapplicableCandidate
import org.jetbrains.kotlin.fir.resolve.calls.MissingInnerClassConstructorReceiver
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CandidateCollector
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CandidateFactory
import org.jetbrains.kotlin.fir.resolve.calls.stages.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.delegatingConstructorScope
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.AbstractTypeChecker

class FirTowerResolver(
    private val components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner,
    private val collector: CandidateCollector = CandidateCollector(components, resolutionStageRunner)
) {
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
        manager: TowerResolveManager,
        candidateFactory: CandidateFactory = CandidateFactory(context, info),
    ): CandidateCollector {
        val candidateFactoriesAndCollectors = CandidateFactoriesAndCollectors(candidateFactory, collector)

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
        // unwrapSmartCastExpression() shouldn't be here, otherwise we can get FirResolvedQualifier instead of 'this' (see e.g. kt2811.kt)
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
                if (receiver is FirSuperReceiverExpression) {
                    manager.enqueueResolverTask { mainTask.runResolverForSuperReceiver(info, receiver) }
                    invokeResolveTowerExtension.enqueueResolveTasksForSuperReceiver(info, receiver)
                    return
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
        derivedClassLookupTag: ConeClassLikeLookupTag,
        context: ResolutionContext
    ): CandidateCollector {
        val outerType = components.outerClassManager.outerType(constructedType)
        val scope =
            constructedType.delegatingConstructorScope(components.session, components.scopeSession, derivedClassLookupTag, outerType)
                ?: return collector

        val dispatchReceiver =
            if (outerType != null)
                components.implicitValueStorage.receiversAsReversed().filterIsInstance<ImplicitReceiverValue<*>>().drop(1).firstOrNull {
                    AbstractTypeChecker.isSubtypeOf(components.session.typeContext, it.type, outerType)
                }
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
                    dispatchReceiver?.receiverExpression,
                    givenExtensionReceiverOptions = emptyList()
                ).apply {
                    if (outerType != null && dispatchReceiver == null) {
                        val diagnostic = constructedType
                            .toRegularClassSymbol(context.session)
                            ?.let(::MissingInnerClassConstructorReceiver)
                            ?: InapplicableCandidate

                        addDiagnostic(diagnostic)
                    }
                },
                context
            )
        }

        return collector
    }

    fun reset() {
        collector.newDataSet()
        manager.reset()
    }
}
