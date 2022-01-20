/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.FirPropertyAccessExpressionBuilder
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConePropertyAsOperator
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class FirInvokeResolveTowerExtension(
    private val context: ResolutionContext,
    private val manager: TowerResolveManager,
    private val candidateFactoriesAndCollectors: CandidateFactoriesAndCollectors
) {
    private val components: BodyResolveComponents
        get() = context.bodyResolveComponents

    fun enqueueResolveTasksForQualifier(info: CallInfo, receiver: FirResolvedQualifier) {
        if (info.callKind != CallKind.Function) return
        enqueueBothInvokeReceiverTasks(
            info,
            invokeAction = { task, receiverInfo ->
                task.runResolverForQualifierReceiver(receiverInfo, receiver)
            },
            invokeExtensionAction = { task, receiverInfo ->
                task.runResolverForNoReceiver(receiverInfo)
            }
        )
    }

    fun enqueueResolveTasksForNoReceiver(info: CallInfo) {
        if (info.callKind != CallKind.Function) return
        val invokeReceiverVariableInfo = info.replaceWithVariableAccess()
        enqueueInvokeReceiverTask(
            info,
            invokeReceiverVariableInfo,
            invokeBuiltinExtensionMode = false
        ) {
            it.runResolverForNoReceiver(invokeReceiverVariableInfo)
        }
    }

    fun enqueueResolveTasksForSuperReceiver(info: CallInfo, receiver: FirQualifiedAccessExpression) {
        if (info.callKind != CallKind.Function) return
        val invokeReceiverVariableInfo = info.replaceWithVariableAccess()
        enqueueInvokeReceiverTask(
            info,
            invokeReceiverVariableInfo,
            invokeBuiltinExtensionMode = false
        ) {
            it.runResolverForSuperReceiver(invokeReceiverVariableInfo, receiver)
        }
    }

    fun enqueueResolveTasksForExpressionReceiver(info: CallInfo, receiver: FirExpression) {
        if (info.callKind != CallKind.Function) return
        enqueueBothInvokeReceiverTasks(
            info,
            invokeAction = { task, receiverInfo ->
                task.runResolverForExpressionReceiver(receiverInfo, receiver)
            },
            invokeExtensionAction = { task, receiverInfo ->
                task.runResolverForNoReceiver(receiverInfo)
            }
        )
    }

    private inline fun enqueueBothInvokeReceiverTasks(
        originalCallInfo: CallInfo,
        crossinline invokeAction: suspend (FirTowerResolveTask, CallInfo) -> Unit,
        crossinline invokeExtensionAction: suspend (FirTowerResolveTask, CallInfo) -> Unit
    ) {
        val invokeReceiverVariableInfo = originalCallInfo.replaceWithVariableAccess()
        val invokeReceiverVariableWithNoReceiverInfo = invokeReceiverVariableInfo.replaceExplicitReceiver(null)

        val towerDataElementsForName = TowerDataElementsForName(invokeReceiverVariableInfo.name, components.towerDataContext)

        enqueueInvokeReceiverTask(
            originalCallInfo,
            invokeReceiverVariableInfo,
            towerDataElementsForName = towerDataElementsForName,
            invokeBuiltinExtensionMode = false
        ) {
            invokeAction(it, invokeReceiverVariableInfo)
        }
        enqueueInvokeReceiverTask(
            originalCallInfo,
            invokeReceiverVariableWithNoReceiverInfo,
            towerDataElementsForName = towerDataElementsForName,
            invokeBuiltinExtensionMode = true
        ) {
            invokeExtensionAction(it, invokeReceiverVariableWithNoReceiverInfo)
        }
    }

    private inline fun enqueueInvokeReceiverTask(
        info: CallInfo,
        invokeReceiverInfo: CallInfo,
        towerDataElementsForName: TowerDataElementsForName = TowerDataElementsForName(invokeReceiverInfo.name, components.towerDataContext),
        invokeBuiltinExtensionMode: Boolean,
        crossinline task: suspend (FirTowerResolveTask) -> Unit
    ) {
        val collector = CandidateCollector(components, components.resolutionStageRunner)
        val invokeReceiverProcessor = InvokeReceiverResolveTask(
            components,
            manager,
            towerDataElementsForName,
            collector,
            CandidateFactory(context, invokeReceiverInfo),
            onSuccessfulLevel = { towerGroup ->
                enqueueResolverTasksForInvokeReceiverCandidates(
                    invokeBuiltinExtensionMode, info,
                    receiverGroup = towerGroup,
                    collector
                )
                collector.newDataSet()
            }
        )
        manager.enqueueResolverTask {
            task(invokeReceiverProcessor)
        }
    }

    private fun enqueueResolverTasksForInvokeReceiverCandidates(
        extensionInvokeOnActualReceiver: Boolean,
        info: CallInfo,
        receiverGroup: TowerGroup,
        collector: CandidateCollector
    ) {
        val invokeBuiltinExtensionMode: Boolean = extensionInvokeOnActualReceiver

        for (invokeReceiverCandidate in collector.bestCandidates()) {
            val symbol = invokeReceiverCandidate.symbol
            if (symbol !is FirCallableSymbol<*> && symbol !is FirClassLikeSymbol<*>) continue

            val isExtensionFunctionType =
                (symbol as? FirCallableSymbol<*>)?.fir?.returnTypeRef?.isExtensionFunctionType(components.session) == true

            val extensionReceiverExpression = invokeReceiverCandidate.extensionReceiverExpression()
            val useImplicitReceiverAsBuiltinInvokeArgument =
                !invokeBuiltinExtensionMode && isExtensionFunctionType &&
                        invokeReceiverCandidate.explicitReceiverKind == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER

            val invokeReceiverExpression =
                components.createExplicitReceiverForInvoke(
                    invokeReceiverCandidate, info, invokeBuiltinExtensionMode, extensionReceiverExpression
                )

            val invokeFunctionInfo =
                info.copy(
                    explicitReceiver = invokeReceiverExpression,
                    name = OperatorNameConventions.INVOKE,
                    isImplicitInvoke = true,
                    candidateForCommonInvokeReceiver = invokeReceiverCandidate.takeUnless { invokeBuiltinExtensionMode }
                ).let {
                    when {
                        invokeBuiltinExtensionMode -> it.withReceiverAsArgument(info.explicitReceiver!!)
                        else -> it
                    }
                }

            val explicitReceiver = ExpressionReceiverValue(invokeReceiverExpression)
            enqueueResolverTasksForInvoke(
                invokeFunctionInfo,
                explicitReceiver,
                invokeBuiltinExtensionMode,
                useImplicitReceiverAsBuiltinInvokeArgument,
                receiverGroup
            )
        }
    }

    private fun enqueueResolverTasksForInvoke(
        invokeFunctionInfo: CallInfo,
        explicitReceiver: ExpressionReceiverValue,
        invokeBuiltinExtensionMode: Boolean,
        useImplicitReceiverAsBuiltinInvokeArgument: Boolean,
        receiverGroup: TowerGroup
    ) {
        val invokeOnGivenReceiverCandidateFactory = CandidateFactory(context, invokeFunctionInfo)
        val task = createInvokeFunctionResolveTask(invokeFunctionInfo, receiverGroup, invokeOnGivenReceiverCandidateFactory)
        if (invokeBuiltinExtensionMode) {
            manager.enqueueResolverTask {
                task.runResolverForBuiltinInvokeExtensionWithExplicitArgument(
                    invokeFunctionInfo, explicitReceiver,
                    TowerGroup.EmptyRoot
                )
            }
        } else {
            if (useImplicitReceiverAsBuiltinInvokeArgument) {
                manager.enqueueResolverTask {
                    task.runResolverForBuiltinInvokeExtensionWithImplicitArgument(
                        invokeFunctionInfo, explicitReceiver,
                        TowerGroup.EmptyRoot
                    )
                }
            }

            manager.enqueueResolverTask {
                task.runResolverForInvoke(
                    invokeFunctionInfo, explicitReceiver,
                    TowerGroup.EmptyRoot
                )
            }
        }
    }

    fun enqueueResolveTasksForImplicitInvokeCall(info: CallInfo, receiverExpression: FirExpression) {
        val explicitReceiverValue = ExpressionReceiverValue(receiverExpression)
        val task = createInvokeFunctionResolveTask(info, TowerGroup.EmptyRoot)
        manager.enqueueResolverTask {
            task.runResolverForInvoke(
                info, explicitReceiverValue,
                TowerGroup.EmptyRoot
            )
        }
        manager.enqueueResolverTask {
            task.runResolverForBuiltinInvokeExtensionWithExplicitArgument(
                info, explicitReceiverValue,
                TowerGroup.EmptyRoot
            )
        }
        manager.enqueueResolverTask {
            task.runResolverForBuiltinInvokeExtensionWithImplicitArgument(
                info, explicitReceiverValue,
                TowerGroup.EmptyRoot
            )
        }
    }

    private fun createInvokeFunctionResolveTask(
        info: CallInfo,
        receiverGroup: TowerGroup,
        candidateFactory: CandidateFactory = candidateFactoriesAndCollectors.candidateFactory
    ): InvokeFunctionResolveTask = InvokeFunctionResolveTask(
        components,
        manager,
        TowerDataElementsForName(info.name, components.towerDataContext),
        receiverGroup,
        candidateFactoriesAndCollectors.resultCollector,
        candidateFactory,
    )
}


private fun BodyResolveComponents.createExplicitReceiverForInvoke(
    candidate: Candidate,
    info: CallInfo,
    invokeBuiltinExtensionMode: Boolean,
    extensionReceiverExpression: FirExpression
): FirExpression {
    return when (val symbol = candidate.symbol) {
        is FirCallableSymbol<*> -> createExplicitReceiverForInvokeByCallable(
            candidate, info, invokeBuiltinExtensionMode, extensionReceiverExpression, symbol
        )
        is FirRegularClassSymbol -> buildResolvedQualifierForClass(symbol, sourceElement = null)
        is FirTypeAliasSymbol -> {
            val type = symbol.fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>().fullyExpandedType(session)
            val expansionRegularClassSymbol = type.lookupTag.toSymbolOrError(session)
            buildResolvedQualifierForClass(expansionRegularClassSymbol, sourceElement = symbol.fir.source)
        }
        else -> throw AssertionError()
    }
}

private fun BodyResolveComponents.createExplicitReceiverForInvokeByCallable(
    candidate: Candidate,
    info: CallInfo,
    invokeBuiltinExtensionMode: Boolean,
    extensionReceiverExpression: FirExpression,
    symbol: FirCallableSymbol<*>
): FirExpression {
    return FirPropertyAccessExpressionBuilder().apply {
        calleeReference = FirNamedReferenceWithCandidate(
            null,
            symbol.callableId.callableName,
            candidate
        )
        dispatchReceiver = candidate.dispatchReceiverExpression()
        this.typeRef = returnTypeCalculator.tryCalculateReturnType(symbol.fir as FirTypedDeclaration)

        if (!invokeBuiltinExtensionMode) {
            extensionReceiver = extensionReceiverExpression
            // NB: this should fix problem in DFA (KT-36014)
            explicitReceiver = info.explicitReceiver
        }

        if (candidate.currentApplicability == CandidateApplicability.PROPERTY_AS_OPERATOR) {
            nonFatalDiagnostics.add(ConePropertyAsOperator(candidate.symbol as FirPropertySymbol))
        }
    }.build().let(::transformQualifiedAccessUsingSmartcastInfo)
}

private class InvokeReceiverResolveTask(
    components: BodyResolveComponents,
    manager: TowerResolveManager,
    towerDataElementsForName: TowerDataElementsForName,
    collector: CandidateCollector,
    candidateFactory: CandidateFactory,
    private val onSuccessfulLevel: (TowerGroup) -> Unit
) : FirTowerResolveTask(
    components,
    manager,
    towerDataElementsForName,
    collector,
    candidateFactory,
) {
    override fun interceptTowerGroup(towerGroup: TowerGroup): TowerGroup =
        towerGroup.InvokeResolvePriority(InvokeResolvePriority.INVOKE_RECEIVER)

    override fun onSuccessfulLevel(towerGroup: TowerGroup) {
        this.onSuccessfulLevel.invoke(towerGroup)
    }
}

private class InvokeFunctionResolveTask(
    components: BodyResolveComponents,
    manager: TowerResolveManager,
    towerDataElementsForName: TowerDataElementsForName,
    private val receiverGroup: TowerGroup,
    collector: CandidateCollector,
    candidateFactory: CandidateFactory,
) : FirBaseTowerResolveTask(
    components,
    manager,
    towerDataElementsForName,
    collector,
    candidateFactory,
) {

    override fun interceptTowerGroup(towerGroup: TowerGroup): TowerGroup {
        val invokeGroup = towerGroup.InvokeResolvePriority(InvokeResolvePriority.COMMON_INVOKE)
        val max = maxOf(invokeGroup, receiverGroup)
        return max.InvokeReceiver(receiverGroup)
    }

    suspend fun runResolverForInvoke(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        parentGroupForInvokeCandidates: TowerGroup
    ) {
        processLevelForRegularInvoke(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, parentGroupForInvokeCandidates.Member,
            ExplicitReceiverKind.DISPATCH_RECEIVER
        )

        enumerateTowerLevels(
            onScope = { scope, group ->
                processLevelForRegularInvoke(
                    scope.toScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                    info, group,
                    ExplicitReceiverKind.EXTENSION_RECEIVER
                )
            },
            onImplicitReceiver = { receiver, group ->
                // NB: companions are processed via implicitReceiverValues!
                processLevelForRegularInvoke(
                    receiver.toMemberScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                    info, group.Member,
                    ExplicitReceiverKind.EXTENSION_RECEIVER
                )
            }
        )
    }

    // Here we already know extension receiver for invoke, and it's stated in info as first argument
    suspend fun runResolverForBuiltinInvokeExtensionWithExplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        parentGroupForInvokeCandidates: TowerGroup
    ) {
        processLevel(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, parentGroupForInvokeCandidates.Member.InvokeResolvePriority(InvokeResolvePriority.INVOKE_EXTENSION),
            ExplicitReceiverKind.DISPATCH_RECEIVER
        )
    }

    // Here we don't know extension receiver for invoke, assuming it's one of implicit receivers
    suspend fun runResolverForBuiltinInvokeExtensionWithImplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        parentGroupForInvokeCandidates: TowerGroup
    ) {
        for ((depth, implicitReceiverValue) in towerDataElementsForName.implicitReceivers) {
            val towerGroup =
                parentGroupForInvokeCandidates
                    .Implicit(depth)
                    .InvokeExtension
                    .InvokeResolvePriority(InvokeResolvePriority.INVOKE_EXTENSION)

            processLevel(
                invokeReceiverValue.toMemberScopeTowerLevel(
                    extensionReceiver = implicitReceiverValue,
                    implicitExtensionInvokeMode = true
                ),
                info, towerGroup,
                ExplicitReceiverKind.DISPATCH_RECEIVER
            )
        }
    }

    private suspend fun processLevelForRegularInvoke(
        towerLevel: SessionBasedTowerLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind
    ) = processLevel(
        towerLevel, callInfo,
        group.InvokeResolvePriority(InvokeResolvePriority.COMMON_INVOKE),
        explicitReceiverKind
    )
}
