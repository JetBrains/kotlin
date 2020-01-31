/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
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
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirStaticScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.FqName
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

    private fun runResolverForQualifierReceiver(
        info: CallInfo,
        collector: CandidateCollector,
        resolvedQualifier: FirResolvedQualifier,
        manager: TowerResolveManager
    ): CandidateCollector {
        val qualifierScopes = if (resolvedQualifier.classId == null) {
            listOf(
                FirExplicitSimpleImportingScope(
                    listOf(
                        buildResolvedImport {
                        delegate = buildImport {
                             importedFqName = FqName.topLevel(info.name)
                            isAllUnder = false
                            }
                        packageFqName = resolvedQualifier.packageFqName
                        }
                    ), session, components.scopeSession
                )
            )
        } else {
            QualifierReceiver(resolvedQualifier).qualifierScopes(session, components.scopeSession)
        }

        for ((depth, qualifierScope) in qualifierScopes.withIndex()) {
            manager.processLevel(
                ScopeTowerLevel(session, components, qualifierScope), info.noStubReceiver(), TowerGroup.Qualifier(depth)
            )
            if (collector.isSuccess()) return collector
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

        manager.processQueuedLevelsForInvoke(groupLimit = TowerGroup.Last)
        return collector
    }

    private fun runResolverForNoReceiver(
        info: CallInfo,
        collector: CandidateCollector,
        manager: TowerResolveManager
    ): CandidateCollector {
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
                    if (collector.isSuccess()) return collector
                }
            }
        }
        val nonEmptyLocalScopes = mutableListOf<FirLocalScope>()
        val emptyTopLevelScopes = mutableSetOf<FirScope>()
        for ((index, localScope) in localScopes.withIndex()) {
            val result = manager.processLevel(
                ScopeTowerLevel(session, components, localScope), info, TowerGroup.Local(index)
            )
            if (collector.isSuccess()) return collector
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
                if (collector.isSuccess()) return collector
                if (implicitResult == ProcessorAction.NONE) {
                    implicitReceiverValuesWithEmptyScopes += implicitReceiverValue
                }
                for ((localIndex, localScope) in nonEmptyLocalScopes.withIndex()) {
                    manager.processLevel(
                        ScopeTowerLevel(
                            session, components, localScope, extensionReceiver = implicitReceiverValue
                        ), info, parentGroup.Local(localIndex)
                    )
                    if (collector.isSuccess()) return collector
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
                    if (collector.isSuccess()) return collector
                }
                for ((topIndex, topLevelScope) in topLevelScopes.withIndex()) {
                    if (topLevelScope in emptyTopLevelScopes) continue
                    val result = manager.processLevel(
                        ScopeTowerLevel(
                            session, components, topLevelScope, extensionReceiver = implicitReceiverValue
                        ), info, parentGroup.Top(topIndex)
                    )
                    if (collector.isSuccess()) return collector
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
            if (collector.isSuccess()) return collector
        }

        manager.processQueuedLevelsForInvoke(groupLimit = TowerGroup.Last)
        return collector
    }

    private fun runResolverForExpressionReceiver(
        info: CallInfo,
        collector: CandidateCollector,
        receiver: FirExpression,
        manager: TowerResolveManager
    ): CandidateCollector {
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
                if (collector.isSuccess()) return collector
            }
        }

        manager.processLevel(
            MemberScopeTowerLevel(
                session, components, dispatchReceiver = explicitReceiverValue, scopeSession = components.scopeSession
            ), info, TowerGroup.Member, ExplicitReceiverKind.DISPATCH_RECEIVER
        )
        if (collector.isSuccess()) return collector

        val shouldProcessExplicitReceiverScopeOnly =
            info.callKind == CallKind.Function && info.explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() != null
        if (shouldProcessExplicitReceiverScopeOnly) {
            // Special case (integer literal type)
            return collector
        }

        val nonEmptyLocalScopes = mutableListOf<FirLocalScope>()
        for ((index, localScope) in localScopes.withIndex()) {
            manager.enqueueLevelForInvoke(
                ScopeTowerLevel(session, components, localScope), info, TowerGroup.Local(index),
                InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION
            )
            val result = manager.processLevel(
                ScopeTowerLevel(
                    session, components, localScope, extensionReceiver = explicitReceiverValue
                ), info, TowerGroup.Local(index), ExplicitReceiverKind.EXTENSION_RECEIVER
            )
            if (collector.isSuccess()) return collector
            if (result != ProcessorAction.NONE) {
                nonEmptyLocalScopes += localScope
            }
        }
        for ((implicitReceiverValue, usableAsValue, depth) in implicitReceivers) {
            if (!usableAsValue) continue
            // NB: companions are processed via implicitReceiverValues!
            val parentGroup = TowerGroup.Implicit(depth)
            manager.enqueueLevelForInvoke(
                MemberScopeTowerLevel(
                    session, components, dispatchReceiver = implicitReceiverValue, scopeSession = components.scopeSession
                ), info, parentGroup.Member, InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION
            )
            manager.processLevel(
                MemberScopeTowerLevel(
                    session, components,
                    dispatchReceiver = implicitReceiverValue, extensionReceiver = explicitReceiverValue,
                    scopeSession = components.scopeSession
                ), info, parentGroup.Member, ExplicitReceiverKind.EXTENSION_RECEIVER
            )
            if (collector.isSuccess()) return collector
            for ((localIndex, localScope) in nonEmptyLocalScopes.withIndex()) {
                manager.enqueueLevelForInvoke(
                    ScopeTowerLevel(
                        session, components, localScope, extensionReceiver = implicitReceiverValue
                    ), info, parentGroup.Local(localIndex), InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION
                )
            }
            for ((implicitDispatchReceiverValue, usable, dispatchDepth) in implicitReceivers) {
                if (!usable) continue
                manager.enqueueLevelForInvoke(
                    MemberScopeTowerLevel(
                        session,
                        components,
                        dispatchReceiver = implicitDispatchReceiverValue,
                        extensionReceiver = implicitReceiverValue,
                        scopeSession = components.scopeSession
                    ), info, parentGroup.Implicit(dispatchDepth),
                    InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION
                )
            }
            for ((topIndex, topLevelScope) in topLevelScopes.withIndex()) {
                manager.enqueueLevelForInvoke(
                    ScopeTowerLevel(
                        session, components, topLevelScope, extensionReceiver = implicitReceiverValue
                    ), info, parentGroup.Top(topIndex), InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION
                )
            }
        }
        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            manager.enqueueLevelForInvoke(
                ScopeTowerLevel(session, components, topLevelScope), info, TowerGroup.Top(index),
                InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION
            )
            manager.processLevel(
                ScopeTowerLevel(
                    session, components, topLevelScope, extensionReceiver = explicitReceiverValue
                ), info, TowerGroup.Top(index), ExplicitReceiverKind.EXTENSION_RECEIVER
            )
            if (collector.isSuccess()) return collector
        }

        manager.processQueuedLevelsForInvoke(groupLimit = TowerGroup.Last)
        return collector
    }

    private fun runResolverForSuperReceiver(
        info: CallInfo,
        collector: CandidateCollector,
        superTypeRef: FirTypeRef,
        manager: TowerResolveManager
    ): CandidateCollector {
        val scope = when (superTypeRef) {
            is FirResolvedTypeRef -> superTypeRef.type.scope(session, components.scopeSession)
            is FirComposedSuperTypeRef -> FirCompositeScope(
                superTypeRef.superTypeRefs.mapNotNullTo(mutableListOf()) { it.type.scope(session, components.scopeSession) }
            )
            else -> null
        } ?: return collector
        manager.processLevel(
            ScopeTowerLevel(
                session, components, scope
            ), info, TowerGroup.Member, explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER
        )
        return collector
    }

    internal fun enqueueResolverForInvoke(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        manager: TowerResolveManager
    ) {
        manager.enqueueLevelForInvoke(
            MemberScopeTowerLevel(
                session, components, dispatchReceiver = invokeReceiverValue, scopeSession = components.scopeSession
            ), info, TowerGroup.Member,
            InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
            ExplicitReceiverKind.DISPATCH_RECEIVER
        )
        for ((index, localScope) in localScopes.withIndex()) {
            manager.enqueueLevelForInvoke(
                ScopeTowerLevel(
                    session, components, localScope, extensionReceiver = invokeReceiverValue
                ), info, TowerGroup.Local(index),
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
                ExplicitReceiverKind.EXTENSION_RECEIVER
            )
        }
        for ((implicitReceiverValue, usable, depth) in implicitReceivers) {
            if (!usable) continue
            // NB: companions are processed via implicitReceiverValues!
            val parentGroup = TowerGroup.Implicit(depth)
            manager.enqueueLevelForInvoke(
                MemberScopeTowerLevel(
                    session, components,
                    dispatchReceiver = implicitReceiverValue, extensionReceiver = invokeReceiverValue,
                    scopeSession = components.scopeSession
                ), info, parentGroup.Member,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
                ExplicitReceiverKind.EXTENSION_RECEIVER
            )
        }
        for ((index, topLevelScope) in topLevelScopes.withIndex()) {
            manager.enqueueLevelForInvoke(
                ScopeTowerLevel(
                    session, components, topLevelScope, extensionReceiver = invokeReceiverValue
                ), info, TowerGroup.Top(index),
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
                ExplicitReceiverKind.EXTENSION_RECEIVER
            )
        }
    }

    // Here we already know extension receiver for invoke, and it's stated in info as first argument
    internal fun enqueueResolverForBuiltinInvokeExtensionWithExplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        manager: TowerResolveManager
    ) {
        manager.enqueueLevelForInvoke(
            MemberScopeTowerLevel(
                session, components, dispatchReceiver = invokeReceiverValue,
                scopeSession = components.scopeSession
            ), info, TowerGroup.Member,
            InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
            ExplicitReceiverKind.DISPATCH_RECEIVER
        )
    }

    // Here we don't know extension receiver for invoke, assuming it's one of implicit receivers
    internal fun enqueueResolverForBuiltinInvokeExtensionWithImplicitArgument(
        info: CallInfo,
        invokeReceiverValue: ExpressionReceiverValue,
        manager: TowerResolveManager
    ) {
        for ((implicitReceiverValue, usable, depth) in implicitReceivers) {
            if (!usable) continue
            val parentGroup = TowerGroup.Implicit(depth)
            manager.enqueueLevelForInvoke(
                MemberScopeTowerLevel(
                    session, components, dispatchReceiver = invokeReceiverValue,
                    extensionReceiver = implicitReceiverValue,
                    implicitExtensionInvokeMode = true,
                    scopeSession = components.scopeSession
                ), info, parentGroup.InvokeExtension,
                InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER,
                ExplicitReceiverKind.DISPATCH_RECEIVER
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

        return when (val receiver = info.explicitReceiver) {
            is FirResolvedQualifier -> runResolverForQualifierReceiver(info, collector, receiver, manager)
            null -> runResolverForNoReceiver(info, collector, manager)
            else -> {
                if (receiver is FirQualifiedAccessExpression) {
                    val calleeReference = receiver.calleeReference
                    if (calleeReference is FirSuperReference) {
                        return runResolverForSuperReceiver(info, collector, receiver.typeRef, manager)
                    }
                }
                runResolverForExpressionReceiver(info, collector, receiver, manager)
            }
        }
    }

    fun reset() {
        collector.newDataSet()
        manager.reset()
    }
}