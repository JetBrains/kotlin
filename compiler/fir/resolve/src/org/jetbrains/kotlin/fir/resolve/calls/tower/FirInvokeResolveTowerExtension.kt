/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.declarations.ContextReceiverGroup
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.FirPropertyAccessExpressionBuilder
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConePropertyAsOperator
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.types.isExtensionFunctionType
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
        val emptyScopesCacheForInvokeReceiver = EmptyScopesCache()
        enqueueResolveForExplicitReceiver(info, emptyScopesCacheForInvokeReceiver) { task, receiverInfo ->
            task.runResolverForQualifierReceiver(receiverInfo, receiver, emptyScopesCacheForInvokeReceiver)
        }
    }

    fun enqueueResolveTasksForNoReceiver(info: CallInfo) {
        if (info.callKind != CallKind.Function) return
        val invokeReceiverVariableInfo = info.replaceWithVariableAccess()
        val emptyScopesCacheForInvokeReceiver = EmptyScopesCache()
        enqueueInvokeReceiverTask(
            info,
            invokeReceiverVariableInfo,
            invokeBuiltinExtensionMode = false,
        ) {
            it.runResolverForNoReceiver(invokeReceiverVariableInfo, emptyScopesCacheForInvokeReceiver)
        }
    }

    fun enqueueResolveTasksForSuperReceiver(info: CallInfo, receiver: FirQualifiedAccessExpression) {
        if (info.callKind != CallKind.Function) return
        val invokeReceiverVariableInfo = info.replaceWithVariableAccess()
        val emptyScopesCacheForSuperReceiver = EmptyScopesCache()
        enqueueInvokeReceiverTask(
            info,
            invokeReceiverVariableInfo,
            invokeBuiltinExtensionMode = false,
        ) {
            it.runResolverForSuperReceiver(invokeReceiverVariableInfo, receiver, emptyScopesCacheForSuperReceiver)
        }
    }

    fun enqueueResolveTasksForExpressionReceiver(info: CallInfo, receiver: FirExpression) {
        if (info.callKind != CallKind.Function) return
        val emptyScopesCacheForInvokeReceiver = EmptyScopesCache()
        enqueueResolveForExplicitReceiver(info, emptyScopesCacheForInvokeReceiver) { task, receiverInfo ->
            task.runResolverForExpressionReceiver(receiverInfo, receiver, emptyScopesCacheForInvokeReceiver)
        }
    }

    /**
     * It's whether Qualifier.f() or expressionReceiver.f(), later we name it as "x.f()"
     *
     * @param originalCallInfo describes whole "x.f()"
     * @param invokeAction runs the process of looking for the receiver "x.f" depending on the kind of "x" (qualifier or expression)
     */
    private inline fun enqueueResolveForExplicitReceiver(
        originalCallInfo: CallInfo,
        emptyScopesCache: EmptyScopesCache,
        crossinline invokeAction: suspend (FirTowerResolveTask, CallInfo) -> Unit
    ) {
        val invokeReceiverVariableInfo = originalCallInfo.replaceWithVariableAccess()

        val towerDataElementsForName = TowerDataElementsForName(invokeReceiverVariableInfo.name, components.towerDataContext)

        enqueueInvokeReceiverTask(
            originalCallInfo,
            invokeReceiverVariableInfo,
            towerDataElementsForName = towerDataElementsForName,
            invokeBuiltinExtensionMode = false,
        ) {
            invokeAction(it, invokeReceiverVariableInfo)
        }

        // Try to find "f" property in the given scopes without explicit receiver and then supply "x" as a first argument for invokeExtension
        val invokeReceiverVariableWithNoReceiverInfo = invokeReceiverVariableInfo.replaceExplicitReceiver(null)
        enqueueInvokeReceiverTask(
            originalCallInfo,
            invokeReceiverVariableWithNoReceiverInfo,
            towerDataElementsForName = towerDataElementsForName,
            invokeBuiltinExtensionMode = true,
        ) {
            it.runResolverForNoReceiver(invokeReceiverVariableWithNoReceiverInfo, emptyScopesCache)
        }
    }

    /**
     * Let we have a call if a form of "x.f()" or "f()"
     *
     * This method enqueues a task (based on runResolutionForInvokeReceiverVariable) that for each successful property enqueues another task
     * that tries to resolve "f()" call itself
     *
     * @param info describes whole "x.f()" or "f()"
     * @param invokeReceiverInfo describes "x.f" or "f" variable (in case of no-receiver call or in case of resolving invokeExtension with "x")
     * @param invokeBuiltinExtensionMode is true only when the original call has a form "x.f()" and invokeReceiverInfo is "f"
     * @param runResolutionForInvokeReceiverVariable runs the process of looking for the receiver ("x.f" or "f") on the given FirTowerResolveTask
     */
    private inline fun enqueueInvokeReceiverTask(
        info: CallInfo,
        invokeReceiverInfo: CallInfo,
        towerDataElementsForName: TowerDataElementsForName = TowerDataElementsForName(invokeReceiverInfo.name, components.towerDataContext),
        invokeBuiltinExtensionMode: Boolean,
        crossinline runResolutionForInvokeReceiverVariable: suspend (FirTowerResolveTask) -> Unit
    ) {
        val emptyScopesCacheForInvokeFunction = EmptyScopesCache()
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
                    collector,
                    emptyScopesCacheForInvokeFunction,
                )
                collector.newDataSet()
            }
        )
        manager.enqueueResolverTask {
            runResolutionForInvokeReceiverVariable(invokeReceiverProcessor)
        }
    }

    private fun enqueueResolverTasksForInvokeReceiverCandidates(
        invokeBuiltinExtensionMode: Boolean,
        info: CallInfo,
        receiverGroup: TowerGroup,
        collector: CandidateCollector,
        emptyScopesCache: EmptyScopesCache,
    ) {
        for (invokeReceiverCandidate in collector.bestCandidates()) {
            val symbol = invokeReceiverCandidate.symbol
            if (symbol !is FirCallableSymbol<*> && symbol !is FirClassLikeSymbol<*>) continue

            val isExtensionFunctionType =
                (symbol as? FirCallableSymbol<*>)?.fir?.returnTypeRef?.isExtensionFunctionType(components.session) == true

            if (invokeBuiltinExtensionMode && !isExtensionFunctionType) {
                continue
            }

            val extensionReceiverExpression = invokeReceiverCandidate.chosenExtensionReceiverExpression()
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
                receiverGroup,
                emptyScopesCache,
            )
        }
    }

    private fun enqueueResolverTasksForInvoke(
        // "x.f.invoke(...)" or "f.invoke(x)" (latter for the invokeExtension calls when "f" has an extension function type)
        invokeFunctionInfo: CallInfo,
        explicitReceiver: ExpressionReceiverValue,
        // Might be true only if initial call had explicit receiver (x.f()) and here we resolve "f.invoke(x)" with given "f"
        invokeBuiltinExtensionMode: Boolean,
        // The call has a form "f(..)" without explicit receiver and "f" has an extension function type
        useImplicitReceiverAsBuiltinInvokeArgument: Boolean,
        receiverGroup: TowerGroup,
        emptyScopesCache: EmptyScopesCache,
    ) {
        val invokeOnGivenReceiverCandidateFactory = CandidateFactory(context, invokeFunctionInfo)
        val task = createInvokeFunctionResolveTask(invokeFunctionInfo, receiverGroup, invokeOnGivenReceiverCandidateFactory)
        if (invokeBuiltinExtensionMode) {
            manager.enqueueResolverTask {
                task.runResolverForBuiltinInvokeExtensionWithExplicitArgument(
                    invokeFunctionInfo, explicitReceiver,
                    TowerGroup.EmptyRoot,
                    emptyScopesCache,
                )
            }
        } else {
            if (useImplicitReceiverAsBuiltinInvokeArgument) {
                require(explicitReceiver.type.fullyExpandedType(context.session).isExtensionFunctionType)
                manager.enqueueResolverTask {
                    task.runResolverForBuiltinInvokeExtensionWithImplicitArgument(
                        invokeFunctionInfo, explicitReceiver,
                        TowerGroup.EmptyRoot,
                        emptyScopesCache,
                    )
                }
            }

            manager.enqueueResolverTask {
                task.runResolverForInvoke(
                    invokeFunctionInfo, explicitReceiver,
                    TowerGroup.EmptyRoot,
                    emptyScopesCache,
                )
            }
        }
    }

    // For calls having a form of "x.(f)()"
    fun enqueueResolveTasksForImplicitInvokeCall(info: CallInfo, receiverExpression: FirExpression, emptyScopesCache: EmptyScopesCache) {
        val explicitReceiverValue = ExpressionReceiverValue(receiverExpression)
        val task = createInvokeFunctionResolveTask(info, TowerGroup.EmptyRoot)
        manager.enqueueResolverTask {
            task.runResolverForInvoke(
                info, explicitReceiverValue,
                TowerGroup.EmptyRoot,
                emptyScopesCache,
            )
        }
        manager.enqueueResolverTask {
            task.runResolverForBuiltinInvokeExtensionWithExplicitArgument(
                info, explicitReceiverValue,
                TowerGroup.EmptyRoot,
                emptyScopesCache,
            )
        }
        manager.enqueueResolverTask {
            task.runResolverForBuiltinInvokeExtensionWithImplicitArgument(
                info, explicitReceiverValue,
                TowerGroup.EmptyRoot,
                emptyScopesCache,
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
        this.typeRef = returnTypeCalculator.tryCalculateReturnType(symbol.fir)

        if (!invokeBuiltinExtensionMode) {
            extensionReceiver = extensionReceiverExpression
            // NB: this should fix problem in DFA (KT-36014)
            explicitReceiver = info.explicitReceiver
        }

        if (candidate.currentApplicability == CandidateApplicability.K2_PROPERTY_AS_OPERATOR) {
            nonFatalDiagnostics.add(ConePropertyAsOperator(candidate.symbol as FirPropertySymbol))
        }
    }.build().let {
        transformQualifiedAccessUsingSmartcastInfo(it)
    }
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
        parentGroupForInvokeCandidates: TowerGroup,
        emptyScopesCache: EmptyScopesCache,
    ) {
        processLevelForRegularInvokeStupid(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, parentGroupForInvokeCandidates.Member,
            ExplicitReceiverKind.DISPATCH_RECEIVER,
//            invokeReceiverValue to emptyScopesCache.invokeReceiverValuesWithEmptyScopes,
        )

        enumerateTowerLevels(
            onScope = { scope, group ->
                processLevelForRegularInvoke(
                    scope.toScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                    info, group,
                    ExplicitReceiverKind.EXTENSION_RECEIVER,
                    scope to emptyScopesCache.emptyScopes,
                )
            },
            onImplicitReceiver = { receiver, group ->
                // NB: companions are processed via implicitReceiverValues!
                processLevelForRegularInvokeStupid(
                    receiver.toMemberScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                    info, group.Member,
                    ExplicitReceiverKind.EXTENSION_RECEIVER,
//                    receiver to emptyScopesCache.implicitReceiverValuesWithEmptyScopes,
                )
            },
            onContextReceiverGroup = { contextReceiverGroup, towerGroup ->
                processLevelForRegularInvokeForContextReceiverGroup(
                    contextReceiverGroup,
                    { it.toMemberScopeTowerLevel(emptyScopesCache, extensionReceiver = invokeReceiverValue) },
                    info, towerGroup.Member,
                    ExplicitReceiverKind.EXTENSION_RECEIVER,
                    emptyScopesCache,
                )
            }
        )
    }

    // Here we already know extension receiver for invoke, and it's stated in info as first argument
    suspend fun runResolverForBuiltinInvokeExtensionWithExplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        parentGroupForInvokeCandidates: TowerGroup,
        emptyScopesCache: EmptyScopesCache,
    ) {
        processLevelStupid(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, parentGroupForInvokeCandidates.Member.InvokeResolvePriority(InvokeResolvePriority.INVOKE_EXTENSION),
            ExplicitReceiverKind.DISPATCH_RECEIVER,
//            invokeReceiverValue to emptyScopesCache.invokeReceiverValuesWithEmptyScopes,
        )
    }

    // Here we don't know extension receiver for invoke, assuming it's one of implicit receivers
    suspend fun runResolverForBuiltinInvokeExtensionWithImplicitArgument(
        // "f.invoke(...)"
        info: CallInfo,
        // "f" should have an extension function type
        invokeReceiverValue: ExpressionReceiverValue,
        parentGroupForInvokeCandidates: TowerGroup,
        emptyScopesCache: EmptyScopesCache,
    ) {
        for ((depth, implicitReceiverValue) in towerDataElementsForName.implicitReceivers) {
            val towerGroup =
                parentGroupForInvokeCandidates
                    .Implicit(depth)
                    .InvokeExtension
                    .InvokeResolvePriority(InvokeResolvePriority.INVOKE_EXTENSION)

            processLevelStupid(
                invokeReceiverValue.toMemberScopeTowerLevel(),
                // Try to supply `implicitReceiverValue` as an "x" in "f.invoke(x)"
                info.withReceiverAsArgument(implicitReceiverValue.receiverExpression), towerGroup,
                ExplicitReceiverKind.DISPATCH_RECEIVER,
//                implicitReceiverValue to emptyScopesCache.implicitReceiverValuesWithEmptyScopes,
            )
        }
    }

    private suspend inline fun <reified T> processLevelForRegularInvoke(
        towerLevel: TowerScopeLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind,
        levelProducerAndCache: Pair<T, MutableSet<T>>,
    ) = processLevel(
        towerLevel, callInfo,
        group.InvokeResolvePriority(InvokeResolvePriority.COMMON_INVOKE),
        explicitReceiverKind,
        levelProducerAndCache,
    )

    private suspend fun processLevelForRegularInvokeStupid(
        towerLevel: TowerScopeLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind,
    ) = processLevelStupid(
        towerLevel, callInfo,
        group.InvokeResolvePriority(InvokeResolvePriority.COMMON_INVOKE),
        explicitReceiverKind,
    )

    private suspend fun processLevelForRegularInvokeForContextReceiverGroup(
        contextReceiverGroup: ContextReceiverGroup,
        buildLevel: (ContextReceiverGroup) -> ContextReceiverGroupMemberScopeTowerLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind,
        emptyScopesCache: EmptyScopesCache,
    ) = processLevelForContextReceiverGroup(
        contextReceiverGroup, buildLevel, callInfo,
        group.InvokeResolvePriority(InvokeResolvePriority.COMMON_INVOKE),
        explicitReceiverKind,
        emptyScopesCache,
    )
}
