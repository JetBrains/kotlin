/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.FirQualifiedAccessExpressionBuilder
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.transformQualifiedAccessUsingSmartcastInfo
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class CallResolutionContext(
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector,
    val invokeReceiverCandidateFactory: CandidateFactory?,
    val invokeBuiltinExtensionReceiverCandidateFactory: CandidateFactory?,
    val stubReceiverCandidateFactory: CandidateFactory?,
    val invokeReceiverCollector: CandidateCollector?
) {
    // TODO: Get rid of the property, storing state here looks like a hack
    internal lateinit var invokeOnGivenReceiverCandidateFactory: CandidateFactory
}

internal class LevelHandler(
    private val info: CallInfo,
    private val explicitReceiverKind: ExplicitReceiverKind,
    private val group: TowerGroup,
    private val callResolutionContext: CallResolutionContext,
    private val manager: TowerResolveManager,
    private val towerResolverSession: FirTowerResolverSession,
    private val components: BodyResolveComponents
) {

    private val resultCollector: CandidateCollector get() = callResolutionContext.resultCollector
    private var processResult = ProcessorAction.NONE

    private fun enqueueResolverTask(group: TowerGroup = TowerGroup.Start, task: suspend () -> Unit) =
        manager.enqueueResolverTask(group, task)
    
    fun handleLevel(towerLevel: SessionBasedTowerLevel, invokeResolveMode: InvokeResolveMode? = null): ProcessorAction {
        val processor =
            TowerScopeLevelProcessor(
                info.explicitReceiver,
                explicitReceiverKind,
                resultCollector,
                // TODO: performance?
                if (invokeResolveMode == InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER)
                    callResolutionContext.invokeOnGivenReceiverCandidateFactory
                else
                    callResolutionContext.candidateFactory,
                group
            )
        when (info.callKind) {
            CallKind.VariableAccess -> {
                towerLevel.processProperties(info.name, processor)

                if (!resultCollector.isSuccess()) {
                    towerLevel.processObjectsAsVariables(info.name, processor)
                }
            }
            CallKind.Function -> {
                val invokeBuiltinExtensionMode =
                    invokeResolveMode == InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION

                if (!invokeBuiltinExtensionMode) {
                    towerLevel.processFunctions(info.name, processor)
                }

                if (invokeResolveMode == InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER ||
                    resultCollector.isSuccess()
                ) {
                    return processResult
                }

                val invokeReceiverProcessor = TowerScopeLevelProcessor(
                    info.explicitReceiver,
                    explicitReceiverKind,
                    callResolutionContext.invokeReceiverCollector!!,
                    if (invokeBuiltinExtensionMode) callResolutionContext.invokeBuiltinExtensionReceiverCandidateFactory!!
                    else callResolutionContext.invokeReceiverCandidateFactory!!,
                    group
                )
                callResolutionContext.invokeReceiverCollector.newDataSet()
                towerLevel.processProperties(info.name, invokeReceiverProcessor)
                towerLevel.processObjectsAsVariables(info.name, invokeReceiverProcessor)

                if (callResolutionContext.invokeReceiverCollector.isSuccess()) {
                    enqueueResolverTasksForInvokeReceiverCandidates(invokeBuiltinExtensionMode)
                }
            }
            CallKind.CallableReference -> {
                val stubReceiver = info.stubReceiver
                if (stubReceiver != null) {
                    val stubReceiverValue = ExpressionReceiverValue(stubReceiver)
                    val stubProcessor = TowerScopeLevelProcessor(
                        info.explicitReceiver,
                        if (towerLevel is MemberScopeTowerLevel && towerLevel.dispatchReceiver is AbstractExplicitReceiver<*>) {
                            ExplicitReceiverKind.DISPATCH_RECEIVER
                        } else {
                            ExplicitReceiverKind.EXTENSION_RECEIVER
                        },
                        resultCollector,
                        callResolutionContext.stubReceiverCandidateFactory!!, group
                    )
                    val towerLevelWithStubReceiver = towerLevel.replaceReceiverValue(stubReceiverValue)
                    towerLevelWithStubReceiver.processFunctionsAndProperties(info.name, stubProcessor)
                    // NB: we don't perform this for implicit Unit
                    if (!resultCollector.isSuccess() && info.explicitReceiver?.typeRef !is FirImplicitBuiltinTypeRef) {
                        towerLevel.processFunctionsAndProperties(info.name, processor)
                    }
                } else {
                    towerLevel.processFunctionsAndProperties(info.name, processor)
                }
            }
            else -> {
                throw AssertionError("Unsupported call kind in tower resolver: ${info.callKind}")
            }
        }
        return processResult
    }

    private fun TowerScopeLevel.processProperties(
        name: Name,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ) {
        processElementsByNameAndStoreResult(TowerScopeLevel.Token.Properties, name, processor)
    }

    private fun TowerScopeLevel.processFunctions(
        name: Name,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ) {
        processElementsByNameAndStoreResult(TowerScopeLevel.Token.Functions, name, processor)
    }

    private fun TowerScopeLevel.processFunctionsAndProperties(
        name: Name, processor: TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ) {
        processFunctions(name, processor)
        processProperties(name, processor)
    }

    private fun TowerScopeLevel.processObjectsAsVariables(
        name: Name, processor: TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ) {
        // Skipping objects when extension receiver is bound to the level
        if (this is ScopeTowerLevel && this.extensionReceiver != null) return

        processElementsByNameAndStoreResult(TowerScopeLevel.Token.Objects, name, processor)
    }

    private fun <T : AbstractFirBasedSymbol<*>> TowerScopeLevel.processElementsByNameAndStoreResult(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        return processElementsByName(token, name, processor).also {
            processResult += it
        }
    }

    private fun enqueueResolverTasksForInvokeReceiverCandidates(invokeBuiltinExtensionMode: Boolean) {
        for (invokeReceiverCandidate in callResolutionContext.invokeReceiverCollector!!.bestCandidates()) {
            val symbol = invokeReceiverCandidate.symbol
            if (symbol !is FirCallableSymbol<*> && symbol !is FirRegularClassSymbol) continue

            val isExtensionFunctionType =
                (symbol as? FirCallableSymbol<*>)?.fir?.returnTypeRef?.isExtensionFunctionType(components.session) == true

            if (invokeBuiltinExtensionMode && !isExtensionFunctionType) {
                continue
            }

            val extensionReceiverExpression = invokeReceiverCandidate.extensionReceiverExpression()
            val useImplicitReceiverAsBuiltinInvokeArgument =
                !invokeBuiltinExtensionMode && isExtensionFunctionType &&
                        invokeReceiverCandidate.explicitReceiverKind == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER

            val invokeReceiverExpression = components.createExplicitReceiverForInvoke(invokeReceiverCandidate).let {
                if (!invokeBuiltinExtensionMode) {
                    it.extensionReceiver = extensionReceiverExpression
                    // NB: this should fix problem in DFA (KT-36014)
                    it.explicitReceiver = info.explicitReceiver
                    it.safe = info.isSafeCall
                }
                components.transformQualifiedAccessUsingSmartcastInfo(it.build())
            }

            val invokeFunctionInfo =
                info.copy(explicitReceiver = invokeReceiverExpression, name = OperatorNameConventions.INVOKE).let {
                    when {
                        invokeBuiltinExtensionMode -> it.withReceiverAsArgument(info.explicitReceiver!!)
                        else -> it
                    }
                }

            val explicitReceiver = ExpressionReceiverValue(invokeReceiverExpression)
            callResolutionContext.invokeOnGivenReceiverCandidateFactory = CandidateFactory(components, invokeFunctionInfo)
            enqueueResolverTasksForInvoke(
                invokeFunctionInfo,
                explicitReceiver,
                invokeBuiltinExtensionMode,
                useImplicitReceiverAsBuiltinInvokeArgument
            )
        }
    }

    private fun enqueueResolverTasksForInvoke(
        invokeFunctionInfo: CallInfo,
        explicitReceiver: ExpressionReceiverValue,
        invokeBuiltinExtensionMode: Boolean,
        useImplicitReceiverAsBuiltinInvokeArgument: Boolean
    ) {
        if (invokeBuiltinExtensionMode) {
            enqueueResolverTask {
                towerResolverSession.runResolverForBuiltinInvokeExtensionWithExplicitArgument(
                    invokeFunctionInfo, explicitReceiver
                )
            }

            return
        }

        if (useImplicitReceiverAsBuiltinInvokeArgument) {
            enqueueResolverTask {
                towerResolverSession.runResolverForBuiltinInvokeExtensionWithImplicitArgument(
                    invokeFunctionInfo, explicitReceiver
                )
            }
        }

        enqueueResolverTask {
            towerResolverSession.runResolverForInvoke(
                invokeFunctionInfo, explicitReceiver
            )
        }
    }
}

private class TowerScopeLevelProcessor(
    val explicitReceiver: FirExpression?,
    val explicitReceiverKind: ExplicitReceiverKind,
    val resultCollector: CandidateCollector,
    val candidateFactory: CandidateFactory,
    val group: TowerGroup
) : TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>> {
    override fun consumeCandidate(
        symbol: AbstractFirBasedSymbol<*>,
        dispatchReceiverValue: ReceiverValue?,
        implicitExtensionReceiverValue: ImplicitReceiverValue<*>?,
        builtInExtensionFunctionReceiverValue: ReceiverValue?
    ) {
        // Check explicit extension receiver for default package members
        if (symbol is FirNamedFunctionSymbol && dispatchReceiverValue == null &&
            (implicitExtensionReceiverValue == null) != (explicitReceiver == null) &&
            explicitReceiver !is FirResolvedQualifier &&
            symbol.callableId.packageName.startsWith(defaultPackage)
        ) {
            val extensionReceiverType = explicitReceiver?.typeRef?.coneTypeSafe()
                ?: implicitExtensionReceiverValue?.type as? ConeClassLikeType
            if (extensionReceiverType != null) {
                val declarationReceiverTypeRef =
                    (symbol as? FirCallableSymbol<*>)?.fir?.receiverTypeRef as? FirResolvedTypeRef
                val declarationReceiverType = declarationReceiverTypeRef?.type
                if (declarationReceiverType is ConeClassLikeType) {
                    if (!AbstractTypeChecker.isSubtypeOf(
                            candidateFactory.bodyResolveComponents.inferenceComponents.ctx,
                            extensionReceiverType,
                            declarationReceiverType.lookupTag.constructClassType(
                                declarationReceiverType.typeArguments.map { ConeStarProjection }.toTypedArray(),
                                isNullable = true
                            )
                        )
                    ) {
                        return
                    }
                }
            }
        }
        // ---
        resultCollector.consumeCandidate(
            group, candidateFactory.createCandidate(
                symbol,
                explicitReceiverKind,
                dispatchReceiverValue,
                implicitExtensionReceiverValue,
                builtInExtensionFunctionReceiverValue
            )
        )
    }

    companion object {
        val defaultPackage = Name.identifier("kotlin")
    }
}

private fun BodyResolveComponents.createExplicitReceiverForInvoke(candidate: Candidate): FirQualifiedAccessExpressionBuilder {
    val (name, typeRef) = when (val symbol = candidate.symbol) {
        is FirCallableSymbol<*> -> {
            symbol.callableId.callableName to returnTypeCalculator.tryCalculateReturnType(symbol.firUnsafe())
        }
        is FirRegularClassSymbol -> {
            symbol.classId.shortClassName to buildResolvedTypeRef {
                type = symbol.constructType(emptyArray(), isNullable = false)
            }
        }
        else -> throw AssertionError()
    }
    return FirQualifiedAccessExpressionBuilder().apply {
        calleeReference = FirNamedReferenceWithCandidate(
            null,
            name,
            candidate
        )
        dispatchReceiver = candidate.dispatchReceiverExpression()
        this.typeRef = typeRef
    }
}
