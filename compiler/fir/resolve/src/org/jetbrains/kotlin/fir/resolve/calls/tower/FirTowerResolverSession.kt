/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.asReversedFrozen
import org.jetbrains.kotlin.fir.declarations.isInner
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.FirQualifiedAccessExpressionBuilder
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.buildResolvedQualifierForClass
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.transformQualifiedAccessUsingSmartcastInfo
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirTowerResolverSession internal constructor(
    private val components: BodyResolveComponents,
    private val manager: TowerResolveManager,
    private val candidateFactoriesAndCollectors: CandidateFactoriesAndCollectors,
    private val mainCallInfo: CallInfo,
) {
    private data class ImplicitReceiver(
        val receiver: ImplicitReceiverValue<*>,
        val depth: Int
    )

    private val session: FirSession get() = components.session

    private val localScopes: List<FirScope> by lazy(LazyThreadSafetyMode.NONE) {
        val localScopesBase = components.towerDataContext.localScopes
        val result = ArrayList<FirScope>()
        for (i in localScopesBase.lastIndex downTo 0) {
            val localScope = localScopesBase[i]
            if (localScope.mayContainName(mainCallInfo.name)
                || (mainCallInfo.callKind == CallKind.Function && localScope.mayContainName(OperatorNameConventions.INVOKE))
            ) {
                result.add(localScope)
            }
        }

        result
    }

    private val nonLocalTowerDataElements = components.towerDataContext.nonLocalTowerDataElements.asReversedFrozen()

    private val implicitReceivers: List<ImplicitReceiver> by lazy(LazyThreadSafetyMode.NONE) {
        nonLocalTowerDataElements.withIndex().mapNotNull { (index, element) ->
            element.implicitReceiver?.let { ImplicitReceiver(it, index) }
        }
    }

    fun runResolutionForDelegatingConstructor(info: CallInfo, constructorClassSymbol: FirClassSymbol<*>) {
        manager.enqueueResolverTask { runResolverForDelegatingConstructorCall(info, constructorClassSymbol) }
    }

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
        candidateFactory: CandidateFactory = candidateFactoriesAndCollectors.candidateFactory,
        // Non-trivial only for qualifier receiver, because there we should prioritize invokes that were found
        // at Qualifier scopes above candidates found in QualifierAsExpression receiver
        useParentGroupForInvokes: Boolean = false
    ): ProcessorAction {
        manager.requestGroup(group)

        val levelHandler = TowerLevelHandler()

        return levelHandler.handleLevel(
            callInfo, explicitReceiverKind, group,
            candidateFactoriesAndCollectors, towerLevel, invokeResolveMode, candidateFactory
        ) {
            enqueueResolverTasksForInvokeReceiverCandidates(
                invokeResolveMode, callInfo,
                parentGroupForInvokeCandidates = if (useParentGroupForInvokes) group else TowerGroup.EmptyRoot
            )
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
        includeInnerConstructors: Boolean = true
    ): ScopeTowerLevel = ScopeTowerLevel(
        session, components, this,
        extensionReceiver, extensionsOnly, includeInnerConstructors
    )

    private fun FirScope.toConstructorScopeTowerLevel(): ConstructorScopeTowerLevel =
        ConstructorScopeTowerLevel(session, this)

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
                qualifierScope.toScopeTowerLevel(includeInnerConstructors = false),
                info.noStubReceiver(), TowerGroup.Qualifier(depth),
                useParentGroupForInvokes = true,
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
        val group = if (prioritized) TowerGroup.ClassifierPrioritized else TowerGroup.Classifier
        processLevel(
            scope.toScopeTowerLevel(includeInnerConstructors = false), info.noStubReceiver(),
            group,
            useParentGroupForInvokes = true,
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

    private suspend fun runResolverForDelegatingConstructorCall(info: CallInfo, constructorClassSymbol: FirClassSymbol<*>) {
        val scope = constructorClassSymbol.fir.unsubstitutedScope(session, components.scopeSession)
        if (constructorClassSymbol is FirRegularClassSymbol && constructorClassSymbol.fir.isInner) {
            // Search for inner constructors only
            for ((implicitReceiverValue, depth) in implicitReceivers.drop(1)) {
                processLevel(
                    implicitReceiverValue.toMemberScopeTowerLevel(),
                    info.copy(name = constructorClassSymbol.fir.name), TowerGroup.Implicit(depth)
                )
            }
        } else {
            // Search for non-inner constructors only
            processLevel(
                scope.toConstructorScopeTowerLevel(),
                info, TowerGroup.Member
            )
        }
    }

    private suspend fun runResolverForNoReceiver(
        info: CallInfo
    ) {
        processExtensionsThatHideMembers(info, explicitReceiverValue = null)
        val nonEmptyLocalScopes = mutableListOf<FirScope>()

        val emptyTopLevelScopes = mutableSetOf<FirScope>()
        val implicitReceiverValuesWithEmptyScopes = mutableSetOf<ImplicitReceiverValue<*>>()

        enumerateTowerLevels(
            onLocalScope = { index, scope ->
                val result = processLevel(
                    scope.toScopeTowerLevel(), info, TowerGroup.Local(index)
                )
                if (result != ProcessorAction.NONE) {
                    nonEmptyLocalScopes += scope
                }
            },
            onNonLocalScope = l@{ index, scope ->
                // NB: this check does not work for variables
                // because we do not search for objects if we have extension receiver
                if (info.callKind != CallKind.VariableAccess && scope in emptyTopLevelScopes) return@l

                processLevel(
                    scope.toScopeTowerLevel(), info, TowerGroup.NonLocal(index)
                )
            },
            onImplicitReceiver = { index, receiver ->
                processCandidatesWithGivenImplicitReceiverAsValue(
                    receiver,
                    info,
                    TowerGroup.Implicit(index),
                    implicitReceiverValuesWithEmptyScopes,
                    nonEmptyLocalScopes,
                    emptyTopLevelScopes
                )
            }
        )
    }

    private suspend fun processExtensionsThatHideMembers(
        info: CallInfo,
        explicitReceiverValue: ReceiverValue?
    ) {
        val shouldProcessExtensionsBeforeMembers =
            info.callKind == CallKind.Function && info.name in HIDES_MEMBERS_NAME_LIST

        if (!shouldProcessExtensionsBeforeMembers) return

        val importingScopes = components.fileImportsScope.asReversed()
        for ((index, topLevelScope) in importingScopes.withIndex()) {
            if (explicitReceiverValue != null) {
                processHideMembersLevel(
                    explicitReceiverValue, topLevelScope, info, index, depth = null,
                    ExplicitReceiverKind.EXTENSION_RECEIVER
                )
            } else {
                for ((implicitReceiverValue, depth) in implicitReceivers) {
                    processHideMembersLevel(
                        implicitReceiverValue, topLevelScope, info, index, depth,
                        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
                    )
                }
            }
        }
    }

    private suspend fun processHideMembersLevel(
        receiverValue: ReceiverValue,
        topLevelScope: FirScope,
        info: CallInfo,
        index: Int,
        depth: Int?,
        explicitReceiverKind: ExplicitReceiverKind
    ) = processLevel(
        topLevelScope.toScopeTowerLevel(
            extensionReceiver = receiverValue, extensionsOnly = true
        ),
        info,
        TowerGroup.TopPrioritized(index).let { if (depth != null) it.Implicit(depth) else it },
        explicitReceiverKind,
    )

    private suspend fun processCandidatesWithGivenImplicitReceiverAsValue(
        receiver: ImplicitReceiverValue<*>,
        info: CallInfo,
        parentGroup: TowerGroup,
        implicitReceiverValuesWithEmptyScopes: MutableSet<ImplicitReceiverValue<*>>,
        nonEmptyLocalScopes: List<FirScope>,
        emptyTopLevelScopes: MutableSet<FirScope>
    ) {
        val implicitResult = processLevel(
            receiver.toMemberScopeTowerLevel(), info, parentGroup.Member
        )

        if (implicitResult == ProcessorAction.NONE) {
            implicitReceiverValuesWithEmptyScopes += receiver
        }

        enumerateTowerLevels(
            localScopesToUse = nonEmptyLocalScopes,
            onLocalScope = { index, scope ->
                processLevel(
                    scope.toScopeTowerLevel(extensionReceiver = receiver),
                    info, parentGroup.Local(index)
                )
            },
            onNonLocalScope = l@{ index, scope ->
                if (scope in emptyTopLevelScopes) return@l

                val result = processLevel(
                    scope.toScopeTowerLevel(extensionReceiver = receiver),
                    info, parentGroup.NonLocal(index)
                )

                if (result == ProcessorAction.NONE) {
                    emptyTopLevelScopes += scope
                }
            },
            onImplicitReceiver = l@{ index, implicitReceiverValue ->
                if (implicitReceiverValue in implicitReceiverValuesWithEmptyScopes) return@l
                processLevel(
                    implicitReceiverValue.toMemberScopeTowerLevel(extensionReceiver = receiver),
                    info, parentGroup.Implicit(index)
                )
            }
        )

    }

    private suspend fun runResolverForExpressionReceiver(
        info: CallInfo,
        receiver: FirExpression
    ) {
        val explicitReceiverValue = ExpressionReceiverValue(receiver)

        processExtensionsThatHideMembers(info, explicitReceiverValue)

        // Member scope of expression receiver
        processLevel(
            explicitReceiverValue.toMemberScopeTowerLevel(), info, TowerGroup.Member, ExplicitReceiverKind.DISPATCH_RECEIVER
        )

        val shouldProcessExplicitReceiverScopeOnly =
            info.callKind == CallKind.Function && info.explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() != null
        if (shouldProcessExplicitReceiverScopeOnly) {
            // Special case (integer literal type)
            return
        }

        val nonEmptyLocalScopes = mutableListOf<FirScope>()

        enumerateTowerLevels(
            onLocalScope = { index, scope ->
                if (processScopeForExplicitReceiver(
                        scope,
                        explicitReceiverValue,
                        info,
                        TowerGroup.Local(index)
                    ) != ProcessorAction.NONE
                ) {
                    nonEmptyLocalScopes += scope
                }
            },
            onNonLocalScope = { index, scope ->
                processScopeForExplicitReceiver(
                    scope,
                    explicitReceiverValue,
                    info,
                    TowerGroup.NonLocal(index)
                )
            },
            onImplicitReceiver = { index, implicitReceiverValue ->
                processCombinationOfReceivers(implicitReceiverValue, explicitReceiverValue, info, index, nonEmptyLocalScopes)
            }
        )
    }

    private inline fun enumerateTowerLevels(
        onLocalScope: (Int, FirScope) -> Unit,
        onNonLocalScope: (Int, FirScope) -> Unit,
        onImplicitReceiver: (Int, ImplicitReceiverValue<*>) -> Unit,
        localScopesToUse: List<FirScope> = localScopes
    ) {
        for ((index, localScope) in localScopesToUse.withIndex()) {
            onLocalScope(index, localScope)
        }

        for ((depth, lexical) in nonLocalTowerDataElements.withIndex()) {
            if (!lexical.isLocal && lexical.scope != null) {
                onNonLocalScope(depth, lexical.scope)
            }

            lexical.implicitReceiver?.let { implicitReceiverValue ->
                onImplicitReceiver(depth, implicitReceiverValue)
            }
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

    private suspend fun processCombinationOfReceivers(
        implicitReceiverValue: ImplicitReceiverValue<*>,
        explicitReceiverValue: ExpressionReceiverValue,
        info: CallInfo,
        depth: Int,
        nonEmptyLocalScopes: MutableList<FirScope>
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

        enumerateTowerLevels(
            localScopesToUse = nonEmptyLocalScopes,
            onLocalScope = { index, scope ->
                processLevelForPropertyWithInvoke(
                    scope.toScopeTowerLevel(extensionReceiver = implicitReceiverValue),
                    info, parentGroup.Local(index)
                )
            },
            onNonLocalScope = { index, scope ->
                processLevelForPropertyWithInvoke(
                    scope.toScopeTowerLevel(extensionReceiver = implicitReceiverValue),
                    info, parentGroup.NonLocal(index)
                )
            },
            onImplicitReceiver = { index, receiver ->
                processLevelForPropertyWithInvoke(
                    receiver.toMemberScopeTowerLevel(extensionReceiver = implicitReceiverValue),
                    info, parentGroup.Implicit(index)
                )
            }
        )
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
        invokeOnGivenReceiverCandidateFactory: CandidateFactory,
        parentGroupForInvokeCandidates: TowerGroup
    ) {
        processLevelForRegularInvoke(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, parentGroupForInvokeCandidates.Member,
            ExplicitReceiverKind.DISPATCH_RECEIVER,
            invokeOnGivenReceiverCandidateFactory
        )

        enumerateTowerLevels(
            onLocalScope = { index, scope ->
                processLevelForRegularInvoke(
                    scope.toScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                    info, parentGroupForInvokeCandidates.Local(index),
                    ExplicitReceiverKind.EXTENSION_RECEIVER,
                    invokeOnGivenReceiverCandidateFactory
                )
            },
            onNonLocalScope = { index, scope ->
                processLevelForRegularInvoke(
                    scope.toScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                    info, parentGroupForInvokeCandidates.NonLocal(index),
                    ExplicitReceiverKind.EXTENSION_RECEIVER,
                    invokeOnGivenReceiverCandidateFactory
                )
            },
            onImplicitReceiver = { index, receiver ->
                // NB: companions are processed via implicitReceiverValues!
                val group = parentGroupForInvokeCandidates.Implicit(index).Member
                processLevelForRegularInvoke(
                    receiver.toMemberScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                    info, group,
                    ExplicitReceiverKind.EXTENSION_RECEIVER,
                    invokeOnGivenReceiverCandidateFactory
                )
            }
        )
    }

    private suspend fun processLevelForRegularInvoke(
        towerLevel: SessionBasedTowerLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind,
        candidateFactory: CandidateFactory
    ) = processLevel(
        towerLevel, callInfo,
        group.InvokeResolvePriority(InvokeResolvePriority.COMMON_INVOKE),
        explicitReceiverKind, InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER, candidateFactory
    )

    // Here we already know extension receiver for invoke, and it's stated in info as first argument
    private suspend fun runResolverForBuiltinInvokeExtensionWithExplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        invokeOnGivenReceiverCandidateFactory: CandidateFactory,
        parentGroupForInvokeCandidates: TowerGroup
    ) {
        processLevel(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, parentGroupForInvokeCandidates.Member.InvokeResolvePriority(InvokeResolvePriority.INVOKE_EXTENSION),
            ExplicitReceiverKind.DISPATCH_RECEIVER,
            InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
            invokeOnGivenReceiverCandidateFactory
        )
    }

    // Here we don't know extension receiver for invoke, assuming it's one of implicit receivers
    private suspend fun runResolverForBuiltinInvokeExtensionWithImplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        invokeOnGivenReceiverCandidateFactory: CandidateFactory,
        parentGroupForInvokeCandidates: TowerGroup
    ) {
        for ((implicitReceiverValue, depth) in implicitReceivers) {
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
                ExplicitReceiverKind.DISPATCH_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
                invokeOnGivenReceiverCandidateFactory
            )
        }
    }

    private fun enqueueResolverTasksForInvokeReceiverCandidates(
        invokeResolveMode: InvokeResolveMode?,
        info: CallInfo,
        parentGroupForInvokeCandidates: TowerGroup
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

            val invokeReceiverExpression =
                components.createExplicitReceiverForInvoke(
                    invokeReceiverCandidate, info, invokeBuiltinExtensionMode, extensionReceiverExpression
                )

            val invokeFunctionInfo =
                info.copy(
                    explicitReceiver = invokeReceiverExpression, name = OperatorNameConventions.INVOKE,
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
                parentGroupForInvokeCandidates
            )
        }
    }

    private fun enqueueResolverTasksForInvoke(
        invokeFunctionInfo: CallInfo,
        explicitReceiver: ExpressionReceiverValue,
        invokeBuiltinExtensionMode: Boolean,
        useImplicitReceiverAsBuiltinInvokeArgument: Boolean,
        parentGroupForInvokeCandidates: TowerGroup
    ) {
        val invokeOnGivenReceiverCandidateFactory = CandidateFactory(components, invokeFunctionInfo)
        if (invokeBuiltinExtensionMode) {
            manager.enqueueResolverTask {
                runResolverForBuiltinInvokeExtensionWithExplicitArgument(
                    invokeFunctionInfo, explicitReceiver, invokeOnGivenReceiverCandidateFactory,
                    parentGroupForInvokeCandidates
                )
            }

            return
        }

        if (useImplicitReceiverAsBuiltinInvokeArgument) {
            manager.enqueueResolverTask {
                runResolverForBuiltinInvokeExtensionWithImplicitArgument(
                    invokeFunctionInfo, explicitReceiver, invokeOnGivenReceiverCandidateFactory,
                    parentGroupForInvokeCandidates
                )
            }
        }

        manager.enqueueResolverTask {
            runResolverForInvoke(
                invokeFunctionInfo, explicitReceiver, invokeOnGivenReceiverCandidateFactory,
                parentGroupForInvokeCandidates
            )
        }
    }
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
    return FirQualifiedAccessExpressionBuilder().apply {
        calleeReference = FirNamedReferenceWithCandidate(
            null,
            symbol.callableId.callableName,
            candidate
        )
        dispatchReceiver = candidate.dispatchReceiverExpression()
        this.typeRef = returnTypeCalculator.tryCalculateReturnType(symbol.firUnsafe())

        if (!invokeBuiltinExtensionMode) {
            extensionReceiver = extensionReceiverExpression
            // NB: this should fix problem in DFA (KT-36014)
            explicitReceiver = info.explicitReceiver
            safe = info.isSafeCall
        }
    }.build().let(::transformQualifiedAccessUsingSmartcastInfo)
}
