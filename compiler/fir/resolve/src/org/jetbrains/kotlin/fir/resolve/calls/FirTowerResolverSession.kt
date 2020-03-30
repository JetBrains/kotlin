/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isCompanion
import org.jetbrains.kotlin.fir.declarations.isInner
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.FirQualifiedAccessExpressionBuilder
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.transformQualifiedAccessUsingSmartcastInfo
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirStaticScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirTowerResolverSession internal constructor(
    val components: BodyResolveComponents,
    implicitReceiverValues: List<ImplicitReceiverValue<*>>,
    private val manager: TowerResolveManager,
    private val candidateFactoriesAndCollectors: CandidateFactoriesAndCollectors,
) {
    private data class ImplicitReceiver(
        val receiver: ImplicitReceiverValue<*>,
        val depth: Int,
        val usableAsValue: Boolean
    )

    private val session: FirSession get() = components.session

    private val implicitReceivers: List<ImplicitReceiver>
    private val implicitReceiversUsableAsValues: List<ImplicitReceiver>

    init {
        val (implicitReceivers, implicitReceiversUsableAsValues) = prepareImplicitReceivers(implicitReceiverValues)
        this.implicitReceivers = implicitReceivers
        this.implicitReceiversUsableAsValues = implicitReceiversUsableAsValues
    }

    private val localScopes: List<FirLocalScope> = components.localScopes.asReversed()

    private val topLevelScopes: List<FirScope> =
        if (components.typeParametersScopes.isEmpty())
            components.fileImportsScope.asReversed()
        else
            components.typeParametersScopes.asReversed() + components.fileImportsScope.asReversed()

    fun runResolution(info: CallInfo) {
        when (val receiver = info.explicitReceiver) {
            is FirResolvedQualifier -> manager.enqueueResolverTask { runResolverForQualifierReceiver(info, receiver) }
            null -> manager.enqueueResolverTask { runResolverForNoReceiver(info) }
            else -> run {
                if (receiver is FirQualifiedAccessExpression) {
                    val calleeReference = receiver.calleeReference
                    if (calleeReference is FirSuperReference) {
                        return@run manager.enqueueResolverTask { runResolverForSuperReceiver(info, receiver.typeRef) }
                    }
                }
                manager.enqueueResolverTask { runResolverForExpressionReceiver(info, receiver) }
            }
        }
    }

    private suspend fun processLevel(
        towerLevel: SessionBasedTowerLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        invokeResolveMode: InvokeResolveMode? = null,
        candidateFactory: CandidateFactory = candidateFactoriesAndCollectors.candidateFactory
    ): ProcessorAction {
        manager.requestGroup(group)

        val levelHandler =
            LevelHandler(
            )

        return levelHandler.handleLevel(
            callInfo, explicitReceiverKind, group,
            candidateFactoriesAndCollectors, towerLevel, invokeResolveMode, candidateFactory
        ) {
            enqueueResolverTasksForInvokeReceiverCandidates(
                invokeResolveMode, callInfo
            )
        }.also {
            manager.stopIfSuccess()
        }
    }

    private suspend fun processLevelForPropertyWithInvoke(
        towerLevel: SessionBasedTowerLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
    ) {
        if (callInfo.callKind == CallKind.Function) {
            processLevel(towerLevel, callInfo, group, explicitReceiverKind, InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION)
        }
    }

    private fun FirScope.toScopeTowerLevel(
        extensionReceiver: ReceiverValue? = null,
        extensionsOnly: Boolean = false,
        noInnerConstructors: Boolean = false
    ): ScopeTowerLevel = ScopeTowerLevel(
        session, components, this,
        extensionReceiver, extensionsOnly, noInnerConstructors
    )

    private fun ReceiverValue.toMemberScopeTowerLevel(
        extensionReceiver: ReceiverValue? = null,
        implicitExtensionInvokeMode: Boolean = false
    ) = MemberScopeTowerLevel(
        session, components, this,
        extensionReceiver, implicitExtensionInvokeMode,
        scopeSession = components.scopeSession
    )

    private suspend fun processQualifierScopes(
        info: CallInfo, qualifierReceiver: QualifierReceiver?
    ) {
        if (qualifierReceiver == null) return
        for ((depth, qualifierScope) in qualifierReceiver.callableScopes().withIndex()) {
            processLevel(
                qualifierScope.toScopeTowerLevel(noInnerConstructors = true),
                info.noStubReceiver(), TowerGroup.Qualifier(depth)
            )
        }
    }

    private suspend fun processClassifierScope(
        info: CallInfo, qualifierReceiver: QualifierReceiver?, prioritized: Boolean
    ) {
        if (qualifierReceiver == null) return
        if (info.callKind != CallKind.CallableReference &&
            qualifierReceiver is ClassQualifierReceiver &&
            qualifierReceiver.classSymbol != qualifierReceiver.originalSymbol
        ) return
        val scope = qualifierReceiver.classifierScope() ?: return
        processLevel(
            scope.toScopeTowerLevel(noInnerConstructors = true), info.noStubReceiver(),
            if (prioritized) TowerGroup.ClassifierPrioritized else TowerGroup.Classifier
        )
    }

    private suspend fun runResolverForQualifierReceiver(
        info: CallInfo,
        resolvedQualifier: FirResolvedQualifier
    ) {
        val qualifierReceiver = createQualifierReceiver(resolvedQualifier, session, components.scopeSession)

        when {
            info.isPotentialQualifierPart -> {
                processClassifierScope(info, qualifierReceiver, prioritized = true)
                processQualifierScopes(info, qualifierReceiver)
            }
            else -> {
                processQualifierScopes(info, qualifierReceiver)
                processClassifierScope(info, qualifierReceiver, prioritized = false)
            }
        }

        if (resolvedQualifier.symbol != null) {
            val typeRef = resolvedQualifier.typeRef
            // NB: yet built-in Unit is used for "no-value" type
            if (info.callKind == CallKind.CallableReference) {
                if (info.stubReceiver != null || typeRef !is FirImplicitBuiltinTypeRef) {
                    runResolverForExpressionReceiver(info, resolvedQualifier)
                }
            } else {
                if (typeRef !is FirImplicitBuiltinTypeRef) {
                    runResolverForExpressionReceiver(info, resolvedQualifier)
                }
            }
        }
    }

    private suspend fun runResolverForNoReceiver(
        info: CallInfo
    ) {
        processExtensionsThatHideMembers(info, explicitReceiverValue = null)
        val nonEmptyLocalScopes = mutableListOf<FirLocalScope>()
        processLocalScopesWithNoReceiver(info, nonEmptyLocalScopes)

        val emptyTopLevelScopes = mutableSetOf<FirScope>()
        processImplicitReceiversWithNoExplicit(info, nonEmptyLocalScopes, emptyTopLevelScopes)

        processTopLevelScopesNoReceiver(info, emptyTopLevelScopes)
    }

    private suspend fun processExtensionsThatHideMembers(
        info: CallInfo,
        explicitReceiverValue: ReceiverValue?
    ) {
        val shouldProcessExtensionsBeforeMembers =
            info.callKind == CallKind.Function && info.name in HIDES_MEMBERS_NAME_LIST

        if (!shouldProcessExtensionsBeforeMembers) return

        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            if (explicitReceiverValue != null) {
                processHideMembersLevel(explicitReceiverValue, topLevelScope, info, index, depth = null)
            } else {
                for ((implicitReceiverValue, depth) in implicitReceiversUsableAsValues) {
                    processHideMembersLevel(implicitReceiverValue, topLevelScope, info, index, depth)
                }
            }
        }
    }

    private suspend fun processHideMembersLevel(
        receiverValue: ReceiverValue,
        topLevelScope: FirScope,
        info: CallInfo,
        index: Int,
        depth: Int?
    ) = processLevel(
        topLevelScope.toScopeTowerLevel(
            extensionReceiver = receiverValue, extensionsOnly = true
        ),
        info, TowerGroup.TopPrioritized(index).let { if (depth != null) it.Implicit(depth) else it }
    )

    private suspend fun processLocalScopesWithNoReceiver(
        info: CallInfo,
        nonEmptyLocalScopes: MutableList<FirLocalScope>
    ) {
        for ((index, localScope) in localScopes.withIndex()) {
            val result = processLevel(
                localScope.toScopeTowerLevel(), info, TowerGroup.Local(index)
            )
            if (result != ProcessorAction.NONE) {
                nonEmptyLocalScopes += localScope
            }
        }
    }

    private suspend fun processImplicitReceiversWithNoExplicit(
        info: CallInfo,
        nonEmptyLocalScopes: MutableList<FirLocalScope>,
        emptyTopLevelScopes: MutableSet<FirScope>
    ) {
        val implicitReceiverValuesWithEmptyScopes = mutableSetOf<ImplicitReceiverValue<*>>()
        for ((implicitReceiverValue, depth, usableAsValue) in implicitReceivers) {
            // NB: companions are processed via implicitReceiverValues!
            val parentGroup = TowerGroup.Implicit(depth)

            if (usableAsValue) {
                processCandidatesWithGivenImplicitReceiverAsValue(
                    implicitReceiverValue,
                    info,
                    parentGroup,
                    implicitReceiverValuesWithEmptyScopes,
                    nonEmptyLocalScopes,
                    emptyTopLevelScopes
                )
            }

            if (implicitReceiverValue is ImplicitDispatchReceiverValue) {
                val scope = implicitReceiverValue.scope(session, components.scopeSession)
                if (scope != null) {
                    processLevel(
                        FirStaticScope(scope).toScopeTowerLevel(),
                        info, parentGroup.Static(depth)
                    )
                }
            }
        }
    }

    private suspend fun processCandidatesWithGivenImplicitReceiverAsValue(
        receiver: ImplicitReceiverValue<*>,
        info: CallInfo,
        parentGroup: TowerGroup,
        implicitReceiverValuesWithEmptyScopes: MutableSet<ImplicitReceiverValue<*>>,
        nonEmptyLocalScopes: List<FirLocalScope>,
        emptyTopLevelScopes: MutableSet<FirScope>
    ) {
        val implicitResult = processLevel(
            receiver.toMemberScopeTowerLevel(), info, parentGroup.Member
        )

        if (implicitResult == ProcessorAction.NONE) {
            implicitReceiverValuesWithEmptyScopes += receiver
        }

        for ((localIndex, localScope) in nonEmptyLocalScopes.withIndex()) {
            processLevel(
                localScope.toScopeTowerLevel(extensionReceiver = receiver),
                info, parentGroup.Local(localIndex)
            )
        }

        for ((implicitDispatchReceiverValue, dispatchDepth) in implicitReceiversUsableAsValues) {
            if (implicitDispatchReceiverValue in implicitReceiverValuesWithEmptyScopes) continue
            processLevel(
                implicitDispatchReceiverValue.toMemberScopeTowerLevel(extensionReceiver = receiver),
                info, parentGroup.Implicit(dispatchDepth)
            )
        }

        for ((topIndex, topLevelScope) in topLevelScopes.withIndex()) {
            if (topLevelScope in emptyTopLevelScopes) continue
            val result = processLevel(
                topLevelScope.toScopeTowerLevel(extensionReceiver = receiver),
                info, parentGroup.Top(topIndex)
            )
            if (result == ProcessorAction.NONE) {
                emptyTopLevelScopes += topLevelScope
            }
        }
    }

    private suspend fun processTopLevelScopesNoReceiver(
        info: CallInfo,
        emptyTopLevelScopes: Collection<FirScope>
    ) {
        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            // NB: this check does not work for variables
            // because we do not search for objects if we have extension receiver
            if (info.callKind != CallKind.VariableAccess && topLevelScope in emptyTopLevelScopes) continue
            processLevel(
                topLevelScope.toScopeTowerLevel(), info, TowerGroup.Top(index)
            )
        }
    }

    private suspend fun runResolverForExpressionReceiver(
        info: CallInfo,
        receiver: FirExpression
    ) {
        val explicitReceiverValue = ExpressionReceiverValue(receiver)

        processExtensionsThatHideMembers(info, explicitReceiverValue)
        processMembersForExplicitReceiver(explicitReceiverValue, info)

        val shouldProcessExplicitReceiverScopeOnly =
            info.callKind == CallKind.Function && info.explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() != null
        if (shouldProcessExplicitReceiverScopeOnly) {
            // Special case (integer literal type)
            return
        }

        val nonEmptyLocalScopes = mutableListOf<FirLocalScope>()
        for ((index, localScope) in localScopes.withIndex()) {
            if (processScopeForExplicitReceiver(
                    localScope,
                    explicitReceiverValue,
                    info,
                    TowerGroup.Local(index)
                ) != ProcessorAction.NONE
            ) {
                nonEmptyLocalScopes += localScope
            }
        }

        for ((implicitReceiverValue, depth) in implicitReceiversUsableAsValues) {
            processCombinationOfReceivers(implicitReceiverValue, explicitReceiverValue, info, depth, nonEmptyLocalScopes)
        }

        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            processScopeForExplicitReceiver(
                topLevelScope,
                explicitReceiverValue,
                info,
                TowerGroup.Top(index)
            )
        }
    }

    private suspend fun processScopeForExplicitReceiver(
        scope: FirScope,
        explicitReceiverValue: ExpressionReceiverValue,
        info: CallInfo,
        towerGroup: TowerGroup,
    ): ProcessorAction {
        val result = processLevel(
            scope.toScopeTowerLevel(extensionReceiver = explicitReceiverValue),
            info, towerGroup, ExplicitReceiverKind.EXTENSION_RECEIVER
        )

        processLevelForPropertyWithInvoke(
            scope.toScopeTowerLevel(), info, towerGroup
        )

        return result
    }

    private suspend fun processMembersForExplicitReceiver(
        explicitReceiverValue: ExpressionReceiverValue,
        info: CallInfo
    ) {
        processLevel(
            explicitReceiverValue.toMemberScopeTowerLevel(), info, TowerGroup.Member, ExplicitReceiverKind.DISPATCH_RECEIVER
        )
    }

    private suspend fun processCombinationOfReceivers(
        implicitReceiverValue: ImplicitReceiverValue<*>,
        explicitReceiverValue: ExpressionReceiverValue,
        info: CallInfo,
        depth: Int,
        nonEmptyLocalScopes: MutableList<FirLocalScope>
    ) {
        // NB: companions are processed via implicitReceiverValues!
        val parentGroup = TowerGroup.Implicit(depth)

        // Member extensions
        processLevel(
            implicitReceiverValue.toMemberScopeTowerLevel(extensionReceiver = explicitReceiverValue),
            info, parentGroup.Member, ExplicitReceiverKind.EXTENSION_RECEIVER
        )
        // properties for invoke on implicit receiver
        processLevelForPropertyWithInvoke(
            implicitReceiverValue.toMemberScopeTowerLevel(), info, parentGroup.Member
        )

        for ((localIndex, localScope) in nonEmptyLocalScopes.withIndex()) {
            processLevelForPropertyWithInvoke(
                localScope.toScopeTowerLevel(extensionReceiver = implicitReceiverValue),
                info, parentGroup.Local(localIndex)
            )
        }

        for ((implicitDispatchReceiverValue, dispatchDepth) in implicitReceiversUsableAsValues) {
            processLevelForPropertyWithInvoke(
                implicitDispatchReceiverValue.toMemberScopeTowerLevel(extensionReceiver = implicitReceiverValue),
                info, parentGroup.Implicit(dispatchDepth)
            )
        }

        for ((topIndex, topLevelScope) in topLevelScopes.withIndex()) {
            processLevelForPropertyWithInvoke(
                topLevelScope.toScopeTowerLevel(extensionReceiver = implicitReceiverValue),
                info, parentGroup.Top(topIndex)
            )
        }
    }

    private suspend fun runResolverForSuperReceiver(
        info: CallInfo,
        superTypeRef: FirTypeRef
    ) {
        val scope = when (superTypeRef) {
            is FirResolvedTypeRef -> superTypeRef.type.scope(session, components.scopeSession)
            is FirComposedSuperTypeRef -> FirCompositeScope(
                superTypeRef.superTypeRefs.mapNotNullTo(mutableListOf()) { it.type.scope(session, components.scopeSession) }
            )
            else -> null
        } ?: return
        processLevel(
            scope.toScopeTowerLevel(),
            info, TowerGroup.Member, explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER
        )
    }

    private suspend fun runResolverForInvoke(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        invokeOnGivenReceiverCandidateFactory: CandidateFactory
    ) {
        processLevel(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, TowerGroup.Member,
            ExplicitReceiverKind.DISPATCH_RECEIVER,
            InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
            invokeOnGivenReceiverCandidateFactory
        )

        for ((index, localScope) in localScopes.withIndex()) {
            processLevel(
                localScope.toScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                info, TowerGroup.Local(index),
                ExplicitReceiverKind.EXTENSION_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
                invokeOnGivenReceiverCandidateFactory
            )
        }

        for ((implicitReceiverValue, depth) in implicitReceiversUsableAsValues) {
            // NB: companions are processed via implicitReceiverValues!
            val parentGroup = TowerGroup.Implicit(depth)
            processLevel(
                implicitReceiverValue.toMemberScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                info, parentGroup.Member,
                ExplicitReceiverKind.EXTENSION_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
                invokeOnGivenReceiverCandidateFactory
            )
        }

        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            processLevel(
                topLevelScope.toScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                info, TowerGroup.Top(index),
                ExplicitReceiverKind.EXTENSION_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
                invokeOnGivenReceiverCandidateFactory
            )
        }
    }

    // Here we already know extension receiver for invoke, and it's stated in info as first argument
    private suspend fun runResolverForBuiltinInvokeExtensionWithExplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        invokeOnGivenReceiverCandidateFactory: CandidateFactory
    ) {
        processLevel(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, TowerGroup.Member,
            ExplicitReceiverKind.DISPATCH_RECEIVER,
            InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
            invokeOnGivenReceiverCandidateFactory
        )
    }

    // Here we don't know extension receiver for invoke, assuming it's one of implicit receivers
    private suspend fun runResolverForBuiltinInvokeExtensionWithImplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        invokeOnGivenReceiverCandidateFactory: CandidateFactory
    ) {
        for ((implicitReceiverValue, depth) in implicitReceiversUsableAsValues) {
            val parentGroup = TowerGroup.Implicit(depth)
            processLevel(
                invokeReceiverValue.toMemberScopeTowerLevel(
                    extensionReceiver = implicitReceiverValue,
                    implicitExtensionInvokeMode = true
                ),
                info, parentGroup.InvokeExtension,
                ExplicitReceiverKind.DISPATCH_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
                invokeOnGivenReceiverCandidateFactory
            )
        }
    }

    private fun enqueueResolverTasksForInvokeReceiverCandidates(
        invokeResolveMode: InvokeResolveMode?,
        info: CallInfo,
    ) {
        val invokeBuiltinExtensionMode = invokeResolveMode == InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION

        for (invokeReceiverCandidate in candidateFactoriesAndCollectors.invokeReceiverCollector!!.bestCandidates()) {
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
        val invokeOnGivenReceiverCandidateFactory = CandidateFactory(components, invokeFunctionInfo)
        if (invokeBuiltinExtensionMode) {
            manager.enqueueResolverTask {
                runResolverForBuiltinInvokeExtensionWithExplicitArgument(
                    invokeFunctionInfo, explicitReceiver, invokeOnGivenReceiverCandidateFactory
                )
            }

            return
        }

        if (useImplicitReceiverAsBuiltinInvokeArgument) {
            manager.enqueueResolverTask {
                runResolverForBuiltinInvokeExtensionWithImplicitArgument(
                    invokeFunctionInfo, explicitReceiver, invokeOnGivenReceiverCandidateFactory
                )
            }
        }

        manager.enqueueResolverTask {
            runResolverForInvoke(
                invokeFunctionInfo, explicitReceiver, invokeOnGivenReceiverCandidateFactory
            )
        }
    }

    private companion object {
        private fun prepareImplicitReceivers(
            implicitReceiverValues: List<ImplicitReceiverValue<*>>
        ): Pair<List<ImplicitReceiver>, List<ImplicitReceiver>> {
            var depth = 0
            var firstDispatchValue = true
            val explicitCompanions = mutableListOf<FirRegularClassSymbol>()
            val implicitReceiversUsableAsValues = mutableListOf<ImplicitReceiver>()
            val implicitReceivers = implicitReceiverValues.mapNotNull { receiverValue ->
                val usableAsValue = when (receiverValue) {
                    is ImplicitExtensionReceiverValue -> true
                    is ImplicitDispatchReceiverValue -> {
                        val symbol = receiverValue.boundSymbol
                        val klass = symbol.fir as? FirRegularClass

                        if (!receiverValue.implicitCompanion && klass?.isCompanion == true) {
                            explicitCompanions += klass.symbol
                        }

                        if (firstDispatchValue && !receiverValue.inDelegated) {
                            if (!receiverValue.implicitCompanion &&
                                klass?.isInner == false &&
                                !symbol.classId.isLocal
                            ) {
                                firstDispatchValue = false
                            }
                            true
                        } else {
                            symbol.fir.classKind == ClassKind.OBJECT && !receiverValue.inDelegated
                        }
                    }
                }

                if (receiverValue is ImplicitDispatchReceiverValue && receiverValue.implicitCompanion && receiverValue.boundSymbol in explicitCompanions) {
                    null
                } else {
                    ImplicitReceiver(receiverValue, depth++, usableAsValue).also {
                        if (usableAsValue) {
                            implicitReceiversUsableAsValues.add(it)
                        }
                    }
                }
            }

            return implicitReceivers to implicitReceiversUsableAsValues
        }
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
