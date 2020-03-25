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
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirStaticScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST

class FirTowerResolver(
    val typeCalculator: ReturnTypeCalculator,
    val components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner,
) {
    private val localScopes: List<FirLocalScope> get() = components.localScopes.asReversed()
    private val topLevelScopes: List<FirScope>
        get() {
            if (components.typeParametersScopes.isEmpty()) return components.fileImportsScope.asReversed()
            return components.typeParametersScopes.asReversed() + components.fileImportsScope.asReversed()
        }

    private val session: FirSession get() = components.session
    private val collector = CandidateCollector(components, resolutionStageRunner)
    private val manager = TowerResolveManager(this)
    private lateinit var implicitReceivers: List<ImplicitReceiver>

    private data class ImplicitReceiver(val receiver: ImplicitReceiverValue<*>, val usableAsValue: Boolean, val depth: Int)

    private fun prepareImplicitReceivers(implicitReceiverValues: List<ImplicitReceiverValue<*>>): List<ImplicitReceiver> {
        var depth = 0
        var firstDispatchValue = true
        val explicitCompanions = mutableListOf<FirRegularClassSymbol>()
        implicitReceivers = implicitReceiverValues.mapNotNull {
            val usableAsValue = when (it) {
                is ImplicitExtensionReceiverValue -> true
                is ImplicitDispatchReceiverValue -> {
                    val symbol = it.boundSymbol
                    val klass = symbol.fir as? FirRegularClass
                    if (!it.implicitCompanion && klass?.isCompanion == true) {
                        explicitCompanions += klass.symbol
                    }
                    if (firstDispatchValue && !it.inDelegated) {
                        if (!it.implicitCompanion &&
                            klass?.isInner == false &&
                            !symbol.classId.isLocal
                        ) {
                            firstDispatchValue = false
                        }
                        true
                    } else {
                        symbol.fir.classKind == ClassKind.OBJECT && !it.inDelegated
                    }
                }
            }
            if (it is ImplicitDispatchReceiverValue && it.implicitCompanion && it.boundSymbol in explicitCompanions) null
            else ImplicitReceiver(it, usableAsValue, depth++)
        }
        return implicitReceivers
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
        manager: TowerResolveManager,
        info: CallInfo, qualifierReceiver: QualifierReceiver?
    ) {
        if (qualifierReceiver == null) return
        for ((depth, qualifierScope) in qualifierReceiver.callableScopes().withIndex()) {
            manager.processLevel(
                qualifierScope.toScopeTowerLevel(noInnerConstructors = true),
                info.noStubReceiver(), TowerGroup.Qualifier(depth)
            )
        }
    }

    private suspend fun processClassifierScope(
        manager: TowerResolveManager,
        info: CallInfo, qualifierReceiver: QualifierReceiver?, prioritized: Boolean
    ) {
        if (qualifierReceiver == null) return
        if (info.callKind != CallKind.CallableReference &&
            qualifierReceiver is ClassQualifierReceiver &&
            qualifierReceiver.classSymbol != qualifierReceiver.originalSymbol
        ) return
        val scope = qualifierReceiver.classifierScope() ?: return
        manager.processLevel(
            scope.toScopeTowerLevel(noInnerConstructors = true), info.noStubReceiver(),
            if (prioritized) TowerGroup.ClassifierPrioritized else TowerGroup.Classifier
        )
    }

    private suspend fun runResolverForQualifierReceiver(
        info: CallInfo,
        resolvedQualifier: FirResolvedQualifier,
        manager: TowerResolveManager
    ) {
        val qualifierReceiver = createQualifierReceiver(resolvedQualifier, session, components.scopeSession)

        when {
            info.isPotentialQualifierPart -> {
                processClassifierScope(manager, info, qualifierReceiver, prioritized = true)
                processQualifierScopes(manager, info, qualifierReceiver)
            }
            else -> {
                processQualifierScopes(manager, info, qualifierReceiver)
                processClassifierScope(manager, info, qualifierReceiver, prioritized = false)
            }
        }


        if (resolvedQualifier.symbol != null) {
            val typeRef = resolvedQualifier.typeRef
            // NB: yet built-in Unit is used for "no-value" type
            if (info.callKind == CallKind.CallableReference) {
                if (info.stubReceiver != null || typeRef !is FirImplicitBuiltinTypeRef) {
                    runResolverForExpressionReceiver(info, resolvedQualifier, manager)
                }
            } else {
                if (typeRef !is FirImplicitBuiltinTypeRef) {
                    runResolverForExpressionReceiver(info, resolvedQualifier, manager)
                }
            }
        }

    }

    private suspend fun runResolverForNoReceiver(
        info: CallInfo,
        manager: TowerResolveManager
    ) {
        processExtensionsThatHideMembers(info, manager)
        val nonEmptyLocalScopes = mutableListOf<FirLocalScope>()
        processLocalScopes(manager, info, nonEmptyLocalScopes)

        val emptyTopLevelScopes = mutableSetOf<FirScope>()
        processImplicitReceivers(manager, info, nonEmptyLocalScopes, emptyTopLevelScopes)

        processTopLevelScopes(info, emptyTopLevelScopes, manager)
    }

    private suspend fun processExtensionsThatHideMembers(
        info: CallInfo,
        manager: TowerResolveManager
    ) {
        val shouldProcessExtensionsBeforeMembers =
            info.callKind == CallKind.Function && info.name in HIDES_MEMBERS_NAME_LIST

        if (!shouldProcessExtensionsBeforeMembers) return

        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            for ((implicitReceiverValue, usableAsValue, depth) in implicitReceivers) {
                if (!usableAsValue) continue
                manager.processLevel(
                    topLevelScope.toScopeTowerLevel(
                        extensionReceiver = implicitReceiverValue, extensionsOnly = true
                    ),
                    info, TowerGroup.TopPrioritized(index).Implicit(depth)
                )
            }
        }
    }

    private suspend fun processLocalScopes(
        manager: TowerResolveManager,
        info: CallInfo,
        nonEmptyLocalScopes: MutableList<FirLocalScope>
    ) {
        for ((index, localScope) in localScopes.withIndex()) {
            val result = manager.processLevel(
                localScope.toScopeTowerLevel(), info, TowerGroup.Local(index)
            )
            if (result != ProcessorAction.NONE) {
                nonEmptyLocalScopes += localScope
            }
        }
    }

    private suspend fun processImplicitReceivers(
        manager: TowerResolveManager,
        info: CallInfo,
        nonEmptyLocalScopes: MutableList<FirLocalScope>,
        emptyTopLevelScopes: MutableSet<FirScope>
    ) {
        val implicitReceiverValuesWithEmptyScopes = mutableSetOf<ImplicitReceiverValue<*>>()
        for ((implicitReceiverValue, usableAsValue, depth) in implicitReceivers) {
            // NB: companions are processed via implicitReceiverValues!
            val parentGroup = TowerGroup.Implicit(depth)

            if (usableAsValue) {
                processCandidatesWithGivenImplicitReceiverAsValue(
                    implicitReceiverValue,
                    manager,
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
                    manager.processLevel(
                        FirStaticScope(scope).toScopeTowerLevel(),
                        info, parentGroup.Static(depth)
                    )
                }
            }
        }
    }

    private suspend fun processCandidatesWithGivenImplicitReceiverAsValue(
        receiver: ImplicitReceiverValue<*>,
        manager: TowerResolveManager,
        info: CallInfo,
        parentGroup: TowerGroup,
        implicitReceiverValuesWithEmptyScopes: MutableSet<ImplicitReceiverValue<*>>,
        nonEmptyLocalScopes: List<FirLocalScope>,
        emptyTopLevelScopes: MutableSet<FirScope>
    ) {
        val implicitResult = manager.processLevel(
            receiver.toMemberScopeTowerLevel(), info, parentGroup.Member
        )

        if (implicitResult == ProcessorAction.NONE) {
            implicitReceiverValuesWithEmptyScopes += receiver
        }

        for ((localIndex, localScope) in nonEmptyLocalScopes.withIndex()) {
            manager.processLevel(
                localScope.toScopeTowerLevel(extensionReceiver = receiver),
                info, parentGroup.Local(localIndex)
            )
        }

        for ((implicitDispatchReceiverValue, usable, dispatchDepth) in implicitReceivers) {
            if (!usable) continue
            if (implicitDispatchReceiverValue in implicitReceiverValuesWithEmptyScopes) continue
            manager.processLevel(
                implicitDispatchReceiverValue.toMemberScopeTowerLevel(extensionReceiver = receiver),
                info, parentGroup.Implicit(dispatchDepth)
            )
        }

        for ((topIndex, topLevelScope) in topLevelScopes.withIndex()) {
            if (topLevelScope in emptyTopLevelScopes) continue
            val result = manager.processLevel(
                topLevelScope.toScopeTowerLevel(extensionReceiver = receiver),
                info, parentGroup.Top(topIndex)
            )
            if (result == ProcessorAction.NONE) {
                emptyTopLevelScopes += topLevelScope
            }
        }
    }

    private suspend fun processTopLevelScopes(
        info: CallInfo,
        emptyTopLevelScopes: Collection<FirScope>,
        manager: TowerResolveManager
    ) {
        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            // NB: this check does not work for variables
            // because we do not search for objects if we have extension receiver
            if (info.callKind != CallKind.VariableAccess && topLevelScope in emptyTopLevelScopes) continue
            manager.processLevel(
                topLevelScope.toScopeTowerLevel(), info, TowerGroup.Top(index)
            )
        }
    }

    private suspend fun runResolverForExpressionReceiver(
        info: CallInfo,
        receiver: FirExpression,
        manager: TowerResolveManager
    ) {
        val explicitReceiverValue = ExpressionReceiverValue(receiver)

        val shouldProcessExtensionsBeforeMembers =
            info.callKind == CallKind.Function && info.name in HIDES_MEMBERS_NAME_LIST
        if (shouldProcessExtensionsBeforeMembers) {
            // Special case (extension hides member)
            for ((index, topLevelScope) in topLevelScopes.withIndex()) {
                manager.processLevel(
                    topLevelScope.toScopeTowerLevel(
                        extensionReceiver = explicitReceiverValue, extensionsOnly = true
                    ),
                    info, TowerGroup.TopPrioritized(index), ExplicitReceiverKind.EXTENSION_RECEIVER
                )
            }
        }

        manager.processLevel(
            explicitReceiverValue.toMemberScopeTowerLevel(), info, TowerGroup.Member, ExplicitReceiverKind.DISPATCH_RECEIVER
        )

        val shouldProcessExplicitReceiverScopeOnly =
            info.callKind == CallKind.Function && info.explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() != null
        if (shouldProcessExplicitReceiverScopeOnly) {
            // Special case (integer literal type)
            return
        }

        val nonEmptyLocalScopes = mutableListOf<FirLocalScope>()
        for ((index, localScope) in localScopes.withIndex()) {
            val result = manager.processLevel(
                localScope.toScopeTowerLevel(extensionReceiver = explicitReceiverValue),
                info, TowerGroup.Local(index), ExplicitReceiverKind.EXTENSION_RECEIVER
            )
            if (result != ProcessorAction.NONE) {
                nonEmptyLocalScopes += localScope
            }
            manager.processLevelForPropertyWithInvoke(
                localScope.toScopeTowerLevel(), info, TowerGroup.Local(index)
            )
        }
        for ((implicitReceiverValue, usableAsValue, depth) in implicitReceivers) {
            if (!usableAsValue) continue
            // NB: companions are processed via implicitReceiverValues!
            val parentGroup = TowerGroup.Implicit(depth)

            manager.processLevel(
                implicitReceiverValue.toMemberScopeTowerLevel(extensionReceiver = explicitReceiverValue),
                info, parentGroup.Member, ExplicitReceiverKind.EXTENSION_RECEIVER
            )
            manager.processLevelForPropertyWithInvoke(
                implicitReceiverValue.toMemberScopeTowerLevel(), info, parentGroup.Member
            )
            for ((localIndex, localScope) in nonEmptyLocalScopes.withIndex()) {
                manager.processLevelForPropertyWithInvoke(
                    localScope.toScopeTowerLevel(extensionReceiver = implicitReceiverValue),
                    info, parentGroup.Local(localIndex)
                )
            }
            for ((implicitDispatchReceiverValue, usable, dispatchDepth) in implicitReceivers) {
                if (!usable) continue
                manager.processLevelForPropertyWithInvoke(
                    implicitDispatchReceiverValue.toMemberScopeTowerLevel(extensionReceiver = implicitReceiverValue),
                    info, parentGroup.Implicit(dispatchDepth)
                )
            }
            for ((topIndex, topLevelScope) in topLevelScopes.withIndex()) {
                manager.processLevelForPropertyWithInvoke(
                    topLevelScope.toScopeTowerLevel(extensionReceiver = implicitReceiverValue),
                    info, parentGroup.Top(topIndex)
                )
            }
        }
        for ((index, topLevelScope) in topLevelScopes.withIndex()) {

            manager.processLevel(
                topLevelScope.toScopeTowerLevel(extensionReceiver = explicitReceiverValue),
                info, TowerGroup.Top(index), ExplicitReceiverKind.EXTENSION_RECEIVER
            )
            manager.processLevelForPropertyWithInvoke(
                topLevelScope.toScopeTowerLevel(), info, TowerGroup.Top(index)
            )
        }

    }

    private suspend fun runResolverForSuperReceiver(
        info: CallInfo,
        superTypeRef: FirTypeRef,
        manager: TowerResolveManager
    ) {
        val scope = when (superTypeRef) {
            is FirResolvedTypeRef -> superTypeRef.type.scope(session, components.scopeSession)
            is FirComposedSuperTypeRef -> FirCompositeScope(
                superTypeRef.superTypeRefs.mapNotNullTo(mutableListOf()) { it.type.scope(session, components.scopeSession) }
            )
            else -> null
        } ?: return
        manager.processLevel(
            scope.toScopeTowerLevel(),
            info, TowerGroup.Member, explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER
        )
    }

    internal suspend fun runResolverForInvoke(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        manager: TowerResolveManager
    ) {
        manager.processLevel(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, TowerGroup.Member,
            ExplicitReceiverKind.DISPATCH_RECEIVER,
            InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER
        )
        for ((index, localScope) in localScopes.withIndex()) {
            manager.processLevel(
                localScope.toScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                info, TowerGroup.Local(index),
                ExplicitReceiverKind.EXTENSION_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER
            )
        }
        for ((implicitReceiverValue, usable, depth) in implicitReceivers) {
            if (!usable) continue
            // NB: companions are processed via implicitReceiverValues!
            val parentGroup = TowerGroup.Implicit(depth)
            manager.processLevel(
                implicitReceiverValue.toMemberScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                info, parentGroup.Member,
                ExplicitReceiverKind.EXTENSION_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER
            )
        }
        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            manager.processLevel(
                topLevelScope.toScopeTowerLevel(extensionReceiver = invokeReceiverValue),
                info, TowerGroup.Top(index),
                ExplicitReceiverKind.EXTENSION_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER
            )
        }
    }

    // Here we already know extension receiver for invoke, and it's stated in info as first argument
    internal suspend fun runResolverForBuiltinInvokeExtensionWithExplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        manager: TowerResolveManager
    ) {
        manager.processLevel(
            invokeReceiverValue.toMemberScopeTowerLevel(),
            info, TowerGroup.Member,
            ExplicitReceiverKind.DISPATCH_RECEIVER,
            InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER
        )
    }

    // Here we don't know extension receiver for invoke, assuming it's one of implicit receivers
    internal suspend fun runResolverForBuiltinInvokeExtensionWithImplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        manager: TowerResolveManager
    ) {
        for ((implicitReceiverValue, usable, depth) in implicitReceivers) {
            if (!usable) continue
            val parentGroup = TowerGroup.Implicit(depth)
            manager.processLevel(
                invokeReceiverValue.toMemberScopeTowerLevel(
                    extensionReceiver = implicitReceiverValue,
                    implicitExtensionInvokeMode = true
                ),
                info, parentGroup.InvokeExtension,
                ExplicitReceiverKind.DISPATCH_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER
            )
        }
    }

    fun runResolver(
        implicitReceiverValues: List<ImplicitReceiverValue<*>>,
        info: CallInfo,
        collector: CandidateCollector = this.collector,
        manager: TowerResolveManager = this.manager
    ): CandidateCollector {
        // TODO: add flag receiver / non-receiver position
        prepareImplicitReceivers(implicitReceiverValues)
        val candidateFactory = CandidateFactory(components, info)
        manager.candidateFactory = candidateFactory
        if (info.callKind == CallKind.CallableReference && info.stubReceiver != null) {
            manager.stubReceiverCandidateFactory = candidateFactory.replaceCallInfo(info.replaceExplicitReceiver(info.stubReceiver))
        }
        manager.resultCollector = collector
        if (info.callKind == CallKind.Function) {
            manager.invokeReceiverCollector = CandidateCollector(components, components.resolutionStageRunner)
            manager.invokeReceiverCandidateFactory = CandidateFactory(components, info.replaceWithVariableAccess())
            if (info.explicitReceiver != null) {
                with(manager.invokeReceiverCandidateFactory) {
                    manager.invokeBuiltinExtensionReceiverCandidateFactory = replaceCallInfo(callInfo.replaceExplicitReceiver(null))
                }
            }
        }

        when (val receiver = info.explicitReceiver) {
            is FirResolvedQualifier -> manager.enqueueResolverTask { runResolverForQualifierReceiver(info, receiver, manager) }
            null -> manager.enqueueResolverTask { runResolverForNoReceiver(info, manager) }
            else -> run {
                if (receiver is FirQualifiedAccessExpression) {
                    val calleeReference = receiver.calleeReference
                    if (calleeReference is FirSuperReference) {
                        return@run manager.enqueueResolverTask { runResolverForSuperReceiver(info, receiver.typeRef, manager) }
                    }
                }
                manager.enqueueResolverTask { runResolverForExpressionReceiver(info, receiver, manager) }
            }
        }
        manager.runTasks()
        return collector
    }

    fun reset() {
        collector.newDataSet()
        manager.reset()
    }
}
