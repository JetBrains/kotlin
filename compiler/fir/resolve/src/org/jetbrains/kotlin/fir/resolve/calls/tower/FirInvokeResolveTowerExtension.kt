/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.FirPropertyAccessExpressionBuilder
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeNotFunctionAsOperator
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal class FirInvokeResolveTowerExtension(
    private val context: ResolutionContext,
    private val manager: TowerResolveManager,
    private val candidateFactoriesAndCollectors: CandidateFactoriesAndCollectors
) {
    private val components: BodyResolveComponents
        get() = context.bodyResolveComponents

    fun enqueueResolveTasksForQualifier(info: CallInfo, receiver: FirResolvedQualifier) {
        if (info.callKind != CallKind.Function) return
        enqueueResolveForExplicitReceiver(
            info
        ) { task, receiverInfo ->
            task.runResolverForQualifierReceiver(receiverInfo, receiver)
        }
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
        enqueueResolveForExplicitReceiver(
            info
        ) { task, receiverInfo ->
            task.runResolverForExpressionReceiver(receiverInfo, receiver)
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
        crossinline invokeAction: suspend (FirTowerResolveTask, CallInfo) -> Unit
    ) {
        val invokeReceiverVariableInfo = originalCallInfo.replaceWithVariableAccess()

        val towerDataElementsForName = TowerDataElementsForName(invokeReceiverVariableInfo.name, components.towerDataContext)

        enqueueInvokeReceiverTask(
            originalCallInfo,
            invokeReceiverVariableInfo,
            towerDataElementsForName = towerDataElementsForName,
            invokeBuiltinExtensionMode = false
        ) {
            invokeAction(it, invokeReceiverVariableInfo)
        }

        // Try to find "f" property in the given scopes without explicit receiver and then supply "x" as a first argument for invokeExtension
        val invokeReceiverVariableWithNoReceiverInfo = invokeReceiverVariableInfo.replaceExplicitReceiver(null)
        enqueueInvokeReceiverTask(
            originalCallInfo,
            invokeReceiverVariableWithNoReceiverInfo,
            towerDataElementsForName = towerDataElementsForName,
            invokeBuiltinExtensionMode = true
        ) {
            // Synthetic properties can never have an extension function type
            it.runResolverForNoReceiver(invokeReceiverVariableWithNoReceiverInfo, skipSynthetics = true)
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
            runResolutionForInvokeReceiverVariable(invokeReceiverProcessor)
        }
    }

    private fun enqueueResolverTasksForInvokeReceiverCandidates(
        invokeBuiltinExtensionMode: Boolean,
        info: CallInfo,
        receiverGroup: TowerGroup,
        collector: CandidateCollector
    ) {
        for (invokeReceiverCandidate in collector.bestCandidates()) {
            val symbol = invokeReceiverCandidate.symbol
            if (symbol !is FirCallableSymbol<*> && symbol !is FirClassLikeSymbol<*>) continue

            val isExtensionFunctionType =
                symbol is FirCallableSymbol<*> &&
                        components.returnTypeCalculator.tryCalculateReturnType(symbol).isExtensionFunctionType(components.session)

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
                ) ?: continue

            if (invokeReceiverExpression.resolvedType is ConeErrorType) continue

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
        // "x.f.invoke(...)" or "f.invoke(x)" (latter for the invokeExtension calls when "f" has an extension function type)
        invokeFunctionInfo: CallInfo,
        explicitReceiver: ExpressionReceiverValue,
        // Might be true only if initial call had explicit receiver (x.f()) and here we resolve "f.invoke(x)" with given "f"
        invokeBuiltinExtensionMode: Boolean,
        // The call has a form "f(..)" without explicit receiver and "f" has an extension function type
        useImplicitReceiverAsBuiltinInvokeArgument: Boolean,
        receiverGroup: TowerGroup
    ) {
        val invokeOnGivenReceiverCandidateFactory = CandidateFactory(context, invokeFunctionInfo)
        val task = createInvokeFunctionResolveTask(invokeFunctionInfo, receiverGroup, invokeOnGivenReceiverCandidateFactory)
        if (invokeBuiltinExtensionMode) {
            manager.enqueueResolverTask {
                task.runResolverForBuiltinInvokeExtensionWithExplicitArgument(
                    invokeFunctionInfo, explicitReceiver,
                )
            }
        } else {
            if (useImplicitReceiverAsBuiltinInvokeArgument) {
                if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                    val session = context.session
                    val fullyExpandedType = explicitReceiver.type.fullyExpandedType(session)
                    require(
                        (session.typeApproximator.approximateToSuperType(
                            fullyExpandedType,
                            TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
                        ) ?: fullyExpandedType).isExtensionFunctionType
                    )
                }
                manager.enqueueResolverTask {
                    task.runResolverForBuiltinInvokeExtensionWithImplicitArgument(
                        invokeFunctionInfo, explicitReceiver,
                    )
                }
            }

            manager.enqueueResolverTask {
                task.runResolverForInvoke(
                    invokeFunctionInfo, explicitReceiver,
                )
            }
        }
    }

    // For calls having a form of "x.(f)()"
    fun enqueueResolveTasksForImplicitInvokeCall(info: CallInfo, receiverExpression: FirExpression) {
        val explicitReceiverValue = ExpressionReceiverValue(receiverExpression)
        val task = createInvokeFunctionResolveTask(info, TowerGroup.EmptyRootForInvokeReceiver)
        manager.enqueueResolverTask {
            task.runResolverForInvoke(
                info, explicitReceiverValue,
            )
        }
        manager.enqueueResolverTask {
            task.runResolverForBuiltinInvokeExtensionWithExplicitArgument(
                info, explicitReceiverValue,
            )
        }
        manager.enqueueResolverTask {
            task.runResolverForBuiltinInvokeExtensionWithImplicitArgument(
                info, explicitReceiverValue,
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
    extensionReceiverExpression: FirExpression?
): FirExpression? {
    val notFunctionAsOperatorDiagnostics = runIf (candidate.lowestApplicability == CandidateApplicability.K2_NOT_FUNCTION_AS_OPERATOR) {
        candidate.diagnostics.filterIsInstance<NotFunctionAsOperator>().map { ConeNotFunctionAsOperator(it.symbol) }
    } ?: emptyList()
    return when (val symbol = candidate.symbol) {
        is FirCallableSymbol<*> -> createExplicitReceiverForInvokeByCallable(
            candidate, info, invokeBuiltinExtensionMode, extensionReceiverExpression, symbol, notFunctionAsOperatorDiagnostics
        )
        is FirRegularClassSymbol -> buildResolvedQualifierForClass(
            symbol,
            sourceElement = info.fakeSourceForImplicitInvokeCallReceiver,
            nonFatalDiagnostics = notFunctionAsOperatorDiagnostics,
        )
        is FirTypeAliasSymbol -> {
            val type = symbol.fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>().fullyExpandedType(session)
            val expansionRegularClassSymbol = type.lookupTag.toSymbol(session) ?: return null
            buildResolvedQualifierForClass(
                expansionRegularClassSymbol,
                sourceElement = symbol.fir.source,
                nonFatalDiagnostics = notFunctionAsOperatorDiagnostics,
            )
        }
        else -> throw AssertionError()
    }
}

private fun BodyResolveComponents.createExplicitReceiverForInvokeByCallable(
    candidate: Candidate,
    info: CallInfo,
    invokeBuiltinExtensionMode: Boolean,
    extensionReceiverExpression: FirExpression?,
    symbol: FirCallableSymbol<*>,
    nonFatalDiagnostics: List<ConeNotFunctionAsOperator>,
): FirExpression {
    return FirPropertyAccessExpressionBuilder().apply {
        val fakeSource = info.fakeSourceForImplicitInvokeCallReceiver
        val returnTypeRef = returnTypeCalculator.tryCalculateReturnType(symbol.fir)
        calleeReference = when {
            returnTypeRef is FirErrorTypeRef -> FirErrorReferenceWithCandidate(
                fakeSource, symbol.callableId.callableName, candidate, returnTypeRef.diagnostic,
            )

            candidate.isSuccessful -> FirNamedReferenceWithCandidate(fakeSource, symbol.callableId.callableName, candidate)

            else -> FirErrorReferenceWithCandidate(
                fakeSource, symbol.callableId.callableName, candidate,
                createConeDiagnosticForCandidateWithError(candidate.applicability, candidate),
            )
        }
        dispatchReceiver = candidate.dispatchReceiverExpression()

        coneTypeOrNull = returnTypeRef.type

        if (!invokeBuiltinExtensionMode) {
            extensionReceiver = extensionReceiverExpression
            // NB: this should fix problem in DFA (KT-36014)
            explicitReceiver = info.explicitReceiver
        }

        this.nonFatalDiagnostics.addAll(nonFatalDiagnostics)

        candidate.updateSourcesOfReceivers()

        source = fakeSource
    }.build().let {
        callCompleter.completeCall(it, ResolutionMode.ReceiverResolution)
    }.let {
        transformQualifiedAccessUsingSmartcastInfo(it)
    }
}

private val CallInfo.fakeSourceForImplicitInvokeCallReceiver: KtSourceElement?
    get() = (callSite as? FirFunctionCall)?.calleeReference?.source?.fakeElement(KtFakeSourceElementKind.ImplicitInvokeCall)

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

    private fun TowerGroup.withGivenInvokeReceiverGroup(invokeResolvePriority: InvokeResolvePriority): TowerGroup =
        InvokeReceiver(receiverGroup, invokeResolvePriority)

    suspend fun runResolverForInvoke(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
    ) {
        processLevelForRegularInvoke(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, TowerGroup.Member,
            ExplicitReceiverKind.DISPATCH_RECEIVER
        )

        enumerateTowerLevels(
            onScope = { scope, _, group ->
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
            },
            onContextReceiverGroup = { contextReceiverGroup, towerGroup ->
                processLevelForRegularInvoke(
                    contextReceiverGroup.toMemberScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                    info, towerGroup.Member,
                    ExplicitReceiverKind.EXTENSION_RECEIVER
                )
            }
        )
    }

    // Here we already know extension receiver for invoke, and it's stated in info as first argument
    suspend fun runResolverForBuiltinInvokeExtensionWithExplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
    ) {
        processLevel(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, TowerGroup.Member.withGivenInvokeReceiverGroup(InvokeResolvePriority.INVOKE_EXTENSION),
            ExplicitReceiverKind.DISPATCH_RECEIVER
        )
    }

    // Here we don't know extension receiver for invoke, assuming it's one of implicit receivers
    suspend fun runResolverForBuiltinInvokeExtensionWithImplicitArgument(
        // "f.invoke(...)"
        info: CallInfo,
        // "f" should have an extension function type
        invokeReceiverValue: ExpressionReceiverValue,
    ) {
        for ((depth, implicitReceiverValue) in towerDataElementsForName.implicitReceivers) {
            val towerGroup =
                TowerGroup
                    .Implicit(depth)
                    // see invokeExtensionVsOther2.kt test
                    .InvokeExtensionWithImplicitReceiver
                    .withGivenInvokeReceiverGroup(InvokeResolvePriority.INVOKE_EXTENSION)

            processLevel(
                invokeReceiverValue.toMemberScopeTowerLevel(),
                // Try to supply `implicitReceiverValue` as an "x" in "f.invoke(x)"
                info.withReceiverAsArgument(implicitReceiverValue.receiverExpression), towerGroup,
                ExplicitReceiverKind.DISPATCH_RECEIVER
            )
        }
        for ((depth, contextReceiverGroup) in towerDataElementsForName.contextReceiverGroups) {
            val towerGroup =
                TowerGroup
                    .ContextReceiverGroup(depth)
                    .InvokeExtensionWithImplicitReceiver
                    .withGivenInvokeReceiverGroup(InvokeResolvePriority.INVOKE_EXTENSION)
            val towerLevel = invokeReceiverValue.toMemberScopeTowerLevel()
            // TODO: resolve for all receivers in the group, but implement the ambiguity diagnostics first. See KT-62712 and KT-69709
            contextReceiverGroup.singleOrNull()?.let { contextReceiverValue ->
                processLevel(
                    towerLevel,
                    info.withReceiverAsArgument(contextReceiverValue.receiverExpression), towerGroup,
                    ExplicitReceiverKind.EXTENSION_RECEIVER
                )
            }
        }
    }

    private suspend fun processLevelForRegularInvoke(
        towerLevel: TowerScopeLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind
    ) = processLevel(
        towerLevel, callInfo,
        group.withGivenInvokeReceiverGroup(InvokeResolvePriority.COMMON_INVOKE),
        explicitReceiverKind
    )
}
