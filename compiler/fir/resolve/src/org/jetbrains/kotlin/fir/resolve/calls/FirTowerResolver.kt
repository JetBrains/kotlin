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
    private val topLevelScopes: List<FirScope>,
    private val localScopes: List<FirLocalScope>
) {

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
                    if (firstDispatchValue) {
                        if (!it.implicitCompanion &&
                            klass?.isInner == false &&
                            !symbol.classId.isLocal
                        ) {
                            firstDispatchValue = false
                        }
                        true
                    } else {
                        symbol.fir.classKind == ClassKind.OBJECT
                    }
                }
            }
            if (it is ImplicitDispatchReceiverValue && it.implicitCompanion && it.boundSymbol in explicitCompanions) null
            else ImplicitReceiver(it, usableAsValue, depth++)
        }
        return implicitReceivers
    }

    private suspend fun processQualifierScopes(
        manager: TowerResolveManager,
        info: CallInfo, qualifierReceiver: QualifierReceiver?
    ) {
        if (qualifierReceiver == null) return
        for ((depth, qualifierScope) in qualifierReceiver.callableScopes().withIndex()) {
            manager.processLevel(
                ScopeTowerLevel(session, components, qualifierScope, noInnerConstructors = true),
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
            ScopeTowerLevel(session, components, scope, noInnerConstructors = true), info.noStubReceiver(),
            if (prioritized) TowerGroup.ClassifierPrioritized else TowerGroup.Classifier
        )
    }

    private suspend fun runResolverForQualifierReceiver(
        info: CallInfo,
        collector: CandidateCollector,
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


        if (resolvedQualifier.classId != null) {
            val typeRef = resolvedQualifier.typeRef
            // NB: yet built-in Unit is used for "no-value" type
            if (info.callKind == CallKind.CallableReference) {
                if (info.stubReceiver != null || typeRef !is FirImplicitBuiltinTypeRef) {
                    runResolverForExpressionReceiver(info, collector, resolvedQualifier, manager)
                }
            } else {
                if (typeRef !is FirImplicitBuiltinTypeRef) {
                    runResolverForExpressionReceiver(info, collector, resolvedQualifier, manager)
                }
            }
        }

    }

    private suspend fun runResolverForNoReceiver(
        info: CallInfo,
        collector: CandidateCollector,
        manager: TowerResolveManager
    ) {
        val shouldProcessExtensionsBeforeMembers =
            info.callKind == CallKind.Function && info.name in HIDES_MEMBERS_NAME_LIST
        if (shouldProcessExtensionsBeforeMembers) {
            // Special case (extension hides member)
            for ((index, topLevelScope) in topLevelScopes.withIndex()) {
                for ((implicitReceiverValue, usableAsValue, depth) in implicitReceivers) {
                    if (!usableAsValue) continue
                    manager.processLevel(
                        ScopeTowerLevel(
                            session, components, topLevelScope, extensionReceiver = implicitReceiverValue, extensionsOnly = true
                        ), info, TowerGroup.TopPrioritized(index).Implicit(depth)
                    )
                }
            }
        }
        val nonEmptyLocalScopes = mutableListOf<FirLocalScope>()
        val emptyTopLevelScopes = mutableSetOf<FirScope>()
        for ((index, localScope) in localScopes.withIndex()) {
            val result = manager.processLevel(
                ScopeTowerLevel(session, components, localScope), info, TowerGroup.Local(index)
            )
            if (result != ProcessorAction.NONE) {
                nonEmptyLocalScopes += localScope
            }
        }
        val implicitReceiverValuesWithEmptyScopes = mutableSetOf<ImplicitReceiverValue<*>>()
        for ((implicitReceiverValue, usableAsValue, depth) in implicitReceivers) {
            // NB: companions are processed via implicitReceiverValues!
            val parentGroup = TowerGroup.Implicit(depth)

            if (usableAsValue) {
                val implicitResult = manager.processLevel(
                    MemberScopeTowerLevel(
                        session, components, dispatchReceiver = implicitReceiverValue, scopeSession = components.scopeSession
                    ), info, parentGroup.Member
                )
                if (implicitResult == ProcessorAction.NONE) {
                    implicitReceiverValuesWithEmptyScopes += implicitReceiverValue
                }
                for ((localIndex, localScope) in nonEmptyLocalScopes.withIndex()) {
                    manager.processLevel(
                        ScopeTowerLevel(
                            session, components, localScope, extensionReceiver = implicitReceiverValue
                        ), info, parentGroup.Local(localIndex)
                    )
                }
                for ((implicitDispatchReceiverValue, usable, dispatchDepth) in implicitReceivers) {
                    if (!usable) continue
                    if (implicitDispatchReceiverValue in implicitReceiverValuesWithEmptyScopes) continue
                    manager.processLevel(
                        MemberScopeTowerLevel(
                            session,
                            components,
                            dispatchReceiver = implicitDispatchReceiverValue,
                            extensionReceiver = implicitReceiverValue,
                            scopeSession = components.scopeSession
                        ), info, parentGroup.Implicit(dispatchDepth)
                    )
                }
                for ((topIndex, topLevelScope) in topLevelScopes.withIndex()) {
                    if (topLevelScope in emptyTopLevelScopes) continue
                    val result = manager.processLevel(
                        ScopeTowerLevel(
                            session, components, topLevelScope, extensionReceiver = implicitReceiverValue
                        ), info, parentGroup.Top(topIndex)
                    )
                    if (result == ProcessorAction.NONE) {
                        emptyTopLevelScopes += topLevelScope
                    }
                }
            }

            if (implicitReceiverValue is ImplicitDispatchReceiverValue) {
                val scope = implicitReceiverValue.scope(session, components.scopeSession)
                if (scope != null) {
                    manager.processLevel(
                        ScopeTowerLevel(
                            session,
                            components,
                            FirStaticScope(scope)
                        ), info, parentGroup.Static(depth)
                    )
                }
            }
        }

        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            // NB: this check does not work for variables
            // because we do not search for objects if we have extension receiver
            if (info.callKind != CallKind.VariableAccess && topLevelScope in emptyTopLevelScopes) continue
            manager.processLevel(
                ScopeTowerLevel(session, components, topLevelScope), info, TowerGroup.Top(index)
            )
        }
    }

    private suspend fun runResolverForExpressionReceiver(
        info: CallInfo,
        collector: CandidateCollector,
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
                    ScopeTowerLevel(
                        session, components, topLevelScope, extensionReceiver = explicitReceiverValue, extensionsOnly = true
                    ), info, TowerGroup.TopPrioritized(index), ExplicitReceiverKind.EXTENSION_RECEIVER
                )
            }
        }

        manager.processLevel(
            MemberScopeTowerLevel(
                session, components, dispatchReceiver = explicitReceiverValue, scopeSession = components.scopeSession
            ), info, TowerGroup.Member, ExplicitReceiverKind.DISPATCH_RECEIVER
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
                ScopeTowerLevel(
                    session, components, localScope, extensionReceiver = explicitReceiverValue
                ), info, TowerGroup.Local(index), ExplicitReceiverKind.EXTENSION_RECEIVER
            )
            if (result != ProcessorAction.NONE) {
                nonEmptyLocalScopes += localScope
            }
            manager.processLevelForPropertyWithInvoke(
                ScopeTowerLevel(session, components, localScope), info, TowerGroup.Local(index)
            )
        }
        for ((implicitReceiverValue, usableAsValue, depth) in implicitReceivers) {
            if (!usableAsValue) continue
            // NB: companions are processed via implicitReceiverValues!
            val parentGroup = TowerGroup.Implicit(depth)

            manager.processLevel(
                MemberScopeTowerLevel(
                    session, components,
                    dispatchReceiver = implicitReceiverValue, extensionReceiver = explicitReceiverValue,
                    scopeSession = components.scopeSession
                ), info, parentGroup.Member, ExplicitReceiverKind.EXTENSION_RECEIVER
            )
            manager.processLevelForPropertyWithInvoke(
                MemberScopeTowerLevel(
                    session, components, dispatchReceiver = implicitReceiverValue, scopeSession = components.scopeSession
                ), info, parentGroup.Member
            )
            for ((localIndex, localScope) in nonEmptyLocalScopes.withIndex()) {
                manager.processLevelForPropertyWithInvoke(
                    ScopeTowerLevel(
                        session, components, localScope, extensionReceiver = implicitReceiverValue
                    ), info, parentGroup.Local(localIndex)
                )
            }
            for ((implicitDispatchReceiverValue, usable, dispatchDepth) in implicitReceivers) {
                if (!usable) continue
                manager.processLevelForPropertyWithInvoke(
                    MemberScopeTowerLevel(
                        session,
                        components,
                        dispatchReceiver = implicitDispatchReceiverValue,
                        extensionReceiver = implicitReceiverValue,
                        scopeSession = components.scopeSession
                    ), info, parentGroup.Implicit(dispatchDepth)
                )
            }
            for ((topIndex, topLevelScope) in topLevelScopes.withIndex()) {
                manager.processLevelForPropertyWithInvoke(
                    ScopeTowerLevel(
                        session, components, topLevelScope, extensionReceiver = implicitReceiverValue
                    ), info, parentGroup.Top(topIndex)
                )
            }
        }
        for ((index, topLevelScope) in topLevelScopes.withIndex()) {

            manager.processLevel(
                ScopeTowerLevel(
                    session, components, topLevelScope, extensionReceiver = explicitReceiverValue
                ), info, TowerGroup.Top(index), ExplicitReceiverKind.EXTENSION_RECEIVER
            )
            manager.processLevelForPropertyWithInvoke(
                ScopeTowerLevel(session, components, topLevelScope), info, TowerGroup.Top(index)
            )
        }

    }

    private suspend fun runResolverForSuperReceiver(
        info: CallInfo,
        collector: CandidateCollector,
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
            ScopeTowerLevel(
                session, components, scope
            ), info, TowerGroup.Member, explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER
        )
    }

    internal suspend fun runResolverForInvoke(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        manager: TowerResolveManager
    ) {
        manager.processLevel(
            MemberScopeTowerLevel(
                session, components, dispatchReceiver = invokeReceiverValue, scopeSession = components.scopeSession
            ),
            info, TowerGroup.Member,
            ExplicitReceiverKind.DISPATCH_RECEIVER,
            InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER
        )
        for ((index, localScope) in localScopes.withIndex()) {
            manager.processLevel(
                ScopeTowerLevel(
                    session, components, localScope, extensionReceiver = invokeReceiverValue
                ),
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
                MemberScopeTowerLevel(
                    session, components,
                    dispatchReceiver = implicitReceiverValue, extensionReceiver = invokeReceiverValue,
                    scopeSession = components.scopeSession
                ), info, parentGroup.Member,
                ExplicitReceiverKind.EXTENSION_RECEIVER,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER
            )
        }
        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            manager.processLevel(
                ScopeTowerLevel(
                    session, components, topLevelScope, extensionReceiver = invokeReceiverValue
                ), info, TowerGroup.Top(index),
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
            MemberScopeTowerLevel(
                session, components, dispatchReceiver = invokeReceiverValue,
                scopeSession = components.scopeSession
            ), info, TowerGroup.Member,
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
                MemberScopeTowerLevel(
                    session, components, dispatchReceiver = invokeReceiverValue,
                    extensionReceiver = implicitReceiverValue,
                    implicitExtensionInvokeMode = true,
                    scopeSession = components.scopeSession
                ), info, parentGroup.InvokeExtension,
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
            is FirResolvedQualifier -> manager.enqueueResolverTask { runResolverForQualifierReceiver(info, collector, receiver, manager) }
            null -> manager.enqueueResolverTask { runResolverForNoReceiver(info, collector, manager) }
            else -> run {
                if (receiver is FirQualifiedAccessExpression) {
                    val calleeReference = receiver.calleeReference
                    if (calleeReference is FirSuperReference) {
                        return@run manager.enqueueResolverTask { runResolverForSuperReceiver(info, collector, receiver.typeRef, manager) }
                    }
                }
                manager.enqueueResolverTask { runResolverForExpressionReceiver(info, collector, receiver, manager) }
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