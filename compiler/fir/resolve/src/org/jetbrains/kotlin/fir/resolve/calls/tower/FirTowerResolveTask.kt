/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.declarations.ContextReceiverGroup
import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirWhenSubjectImportingScope
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.util.asReversedFrozen
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST

internal class TowerDataElementsForName(
    name: Name,
    towerDataContext: FirTowerDataContext
) {
    val nonLocalTowerDataElements = towerDataContext.nonLocalTowerDataElements.asReversedFrozen()

    val reversedFilteredLocalScopes by lazy(LazyThreadSafetyMode.NONE) {
        buildList {
            val localScopesBase = towerDataContext.localScopes
            val lastIndex = localScopesBase.lastIndex
            for (i in lastIndex downTo 0) {
                val localScope = localScopesBase[i]
                if (localScope.mayContainName(name)) {
                    add(IndexedValue(lastIndex - i, localScope))
                }
            }
        }
    }
    val implicitReceivers by lazy(LazyThreadSafetyMode.NONE) {
        nonLocalTowerDataElements.mapIndexedNotNull { index, towerDataElement ->
            towerDataElement.implicitReceiver?.let { receiver -> IndexedValue(index, receiver) }
        }
    }

    val contextReceiverGroups by lazy(LazyThreadSafetyMode.NONE) {
        nonLocalTowerDataElements.mapIndexedNotNull { index, towerDataElement ->
            towerDataElement.contextReceiverGroup?.let { receiver -> IndexedValue(index, receiver) }
        }
    }
}

internal abstract class FirBaseTowerResolveTask(
    protected val components: BodyResolveComponents,
    private val manager: TowerResolveManager,
    protected val towerDataElementsForName: TowerDataElementsForName,
    private val collector: CandidateCollector,
    private val candidateFactory: CandidateFactory
) {
    protected val session get() = components.session

    private val handler: TowerLevelHandler = TowerLevelHandler()

    open fun interceptTowerGroup(towerGroup: TowerGroup) = towerGroup
    open fun onSuccessfulLevel(towerGroup: TowerGroup) {}

    protected suspend fun <T> processLevelWithoutCheck(
        towerLevel: TowerScopeLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        levelProducerAndCache: Pair<T, MutableSet<T>>,
    ) {
        val (levelProducer, cache) = levelProducerAndCache
        val isEmpty = processLevel(towerLevel, callInfo, group, explicitReceiverKind)

        if (isEmpty) {
            cache.add(levelProducer)
        }
    }

    protected suspend inline fun processLevelStupid(
        towerLevel: TowerScopeLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
    ) {
        processLevel(towerLevel, callInfo, group, explicitReceiverKind)
    }

    protected suspend fun processLevelWithoutCaching(
        towerLevel: TowerScopeLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
    ) {
        processLevel(towerLevel, callInfo, group, explicitReceiverKind)
    }

    protected suspend fun <T> processLevel(
        towerLevel: TowerScopeLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        levelProducer: T,
        cache: MutableSet<T>,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
    ) {
        if (levelProducer in cache) return

        if (processLevel(towerLevel, callInfo, group, explicitReceiverKind)) {
            cache += levelProducer
        }
    }

    protected suspend inline fun processLevelForContextReceiverGroup(
        contextReceiverGroup: ContextReceiverGroup,
        buildLevel: (ContextReceiverGroup) -> ContextReceiverGroupMemberScopeTowerLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        emptyScopesCache: EmptyScopesCache,
    ) {
        if (contextReceiverGroup.all { it in emptyScopesCache.contextReceivers }) {
            return
        }

        val towerLevel = buildLevel(contextReceiverGroup)
        processLevel(towerLevel, callInfo, group, explicitReceiverKind)

        for (it in contextReceiverGroup) {
            if (it !in towerLevel.nonEmptyReceivers) {
                emptyScopesCache.contextReceivers.add(it)
            }
        }
    }

    protected fun FirScope.toScopeTowerLevel(
        extensionReceiver: ReceiverValue? = null,
        withHideMembersOnly: Boolean = false,
        includeInnerConstructors: Boolean = extensionReceiver != null,
        contextReceiverGroup: ContextReceiverGroup? = null,
    ): ScopeTowerLevel = ScopeTowerLevel(
        components, this,
        givenExtensionReceiverOptions = contextReceiverGroup ?: listOfNotNull(extensionReceiver),
        withHideMembersOnly, includeInnerConstructors
    )

    protected fun ReceiverValue.toMemberScopeTowerLevel(
        extensionReceiver: ReceiverValue? = null,
        contextReceiverGroup: ContextReceiverGroup? = null,
        givenExtensionReceiverCameFromImplicitReceiver: Boolean = false,
    ) = MemberScopeTowerLevel(
        components, this,
        givenExtensionReceiverOptions = contextReceiverGroup ?: listOfNotNull(extensionReceiver),
        givenExtensionReceiverCameFromImplicitReceiver,
    )

    protected fun ContextReceiverGroup.toMemberScopeTowerLevel(
        emptyScopesCache: EmptyScopesCache,
        extensionReceiver: ReceiverValue? = null,
        otherContextReceiverGroup: ContextReceiverGroup? = null,
    ) = ContextReceiverGroupMemberScopeTowerLevel(
        components, this,
        givenExtensionReceiverOptions = otherContextReceiverGroup ?: listOfNotNull(extensionReceiver),
        emptyScopesCache = emptyScopesCache,
    )

    protected inline fun enumerateTowerLevels(
        parentGroup: TowerGroup = TowerGroup.EmptyRoot,
        onScope: (FirScope, TowerGroup) -> Unit,
        onImplicitReceiver: (ImplicitReceiverValue<*>, TowerGroup) -> Unit,
        onContextReceiverGroup: (ContextReceiverGroup, TowerGroup) -> Unit,
    ) {
        for ((index, localScope) in towerDataElementsForName.reversedFilteredLocalScopes) {
            onScope(localScope, parentGroup.Local(index))
        }

        for ((depth, lexical) in towerDataElementsForName.nonLocalTowerDataElements.withIndex()) {
            val scope = lexical.scope
            if (!lexical.isLocal && scope != null) {
                onScope(
                    scope,
                    if (scope is FirWhenSubjectImportingScope) TowerGroup.UnqualifiedEnum(depth)
                    else parentGroup.NonLocal(depth)
                )
            }

            val receiver = lexical.implicitReceiver

            if (receiver != null && receiver !is InaccessibleImplicitReceiverValue) {
                onImplicitReceiver(receiver, parentGroup.Implicit(depth))
            }
        }

        for ((depth, contextReceiverGroup) in towerDataElementsForName.contextReceiverGroups) {
            onContextReceiverGroup(contextReceiverGroup, parentGroup.ContextReceiverGroup(depth))
        }
    }

    /**
     * @return true if level is empty
     */
    private suspend fun processLevel(
        towerLevel: TowerScopeLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind
    ): Boolean {
        val finalGroup = interceptTowerGroup(group)
        manager.requestGroup(finalGroup)


        val result = handler.handleLevel(
            collector,
            candidateFactory,
            callInfo,
            explicitReceiverKind,
            finalGroup,
            towerLevel
        )
        if (collector.isSuccess) onSuccessfulLevel(finalGroup)
        return result == ProcessResult.SCOPE_EMPTY
    }
}

internal open class FirTowerResolveTask(
    components: BodyResolveComponents,
    manager: TowerResolveManager,
    towerDataElementsForName: TowerDataElementsForName,
    collector: CandidateCollector,
    candidateFactory: CandidateFactory,
) : FirBaseTowerResolveTask(
    components,
    manager,
    towerDataElementsForName,
    collector,
    candidateFactory,
) {

    suspend fun runResolverForQualifierReceiver(
        info: CallInfo,
        resolvedQualifier: FirResolvedQualifier,
        emptyScopesCache: EmptyScopesCache,
    ) {
        val qualifierReceiver = createQualifierReceiver(resolvedQualifier, session, components.scopeSession)

        processQualifierScopes(info, qualifierReceiver, emptyScopesCache)
        processClassifierScope(info, qualifierReceiver, emptyScopesCache)

        if (resolvedQualifier.symbol != null) {
            val typeRef = resolvedQualifier.typeRef
            if (info.callKind == CallKind.CallableReference && info.lhs is DoubleColonLHS.Type) {
                val stubReceiver = buildExpressionStub {
                    source = info.explicitReceiver?.source
                    this.typeRef = buildResolvedTypeRef {
                        type = info.lhs.type
                    }
                }

                val stubReceiverInfo = info.replaceExplicitReceiver(stubReceiver)

                runResolverForExpressionReceiver(
                    stubReceiverInfo,
                    stubReceiver,
                    parentGroup = TowerGroup.QualifierValue,
                    emptyScopesCache = emptyScopesCache,
                )
            }

            // NB: yet built-in Unit is used for "no-value" type
            if (typeRef !is FirImplicitBuiltinTypeRef) {
                runResolverForExpressionReceiver(
                    info,
                    resolvedQualifier,
                    parentGroup = TowerGroup.QualifierValue,
                    emptyScopesCache = emptyScopesCache,
                )
            }

        }
    }

    private suspend fun processQualifierScopes(
        info: CallInfo,
        qualifierReceiver: QualifierReceiver?,
        emptyScopesCache: EmptyScopesCache,
    ) {
        if (qualifierReceiver == null) return
        val callableScope = qualifierReceiver.callableScope() ?: return
        processLevel(
            callableScope.toScopeTowerLevel(includeInnerConstructors = false),
            info, TowerGroup.Qualifier,
            levelProducer = callableScope,
            cache = emptyScopesCache.emptyScopes,
        )
    }

    private suspend fun processClassifierScope(
        info: CallInfo,
        qualifierReceiver: QualifierReceiver?,
        emptyScopesCache: EmptyScopesCache,
    ) {
        if (qualifierReceiver == null) return
        if (info.callKind != CallKind.CallableReference &&
            qualifierReceiver is ClassQualifierReceiver &&
            qualifierReceiver.classSymbol != qualifierReceiver.originalSymbol
        ) return
        val scope = qualifierReceiver.classifierScope() ?: return
        processLevel(
            scope.toScopeTowerLevel(includeInnerConstructors = false), info,
            TowerGroup.Classifier,
            levelProducer = scope,
            cache = emptyScopesCache.emptyScopes,
        )
    }

    suspend fun runResolverForExpressionReceiver(
        info: CallInfo,
        receiver: FirExpression,
        emptyScopesCache: EmptyScopesCache,
        parentGroup: TowerGroup = TowerGroup.EmptyRoot,
    ) {
        val explicitReceiverValue = ExpressionReceiverValue(receiver)

        processExtensionsThatHideMembers(info, explicitReceiverValue, parentGroup, emptyScopesCache)

        // Member scope of expression receiver
        processLevel(
            explicitReceiverValue.toMemberScopeTowerLevel(), info, parentGroup.Member,
            receiver, emptyScopesCache.explicitReceivers,
            ExplicitReceiverKind.DISPATCH_RECEIVER,
        )

        enumerateTowerLevels(
            parentGroup = parentGroup,
            onScope = { scope, group ->
                processScopeForExplicitReceiver(
                    scope,
                    explicitReceiverValue,
                    info,
                    group,
                    emptyScopesCache,
                )
            },
            onImplicitReceiver = { implicitReceiverValue, group ->
                // Member extensions
                processLevel(
                    implicitReceiverValue.toMemberScopeTowerLevel(extensionReceiver = explicitReceiverValue),
                    info, group.Member,
                    implicitReceiverValue,
                    emptyScopesCache.implicitReceiverValuesWithEmptyScopes,
                    ExplicitReceiverKind.EXTENSION_RECEIVER,
                )
            },
            onContextReceiverGroup = { contextReceiverGroup, towerGroup ->
                processLevelForContextReceiverGroup(
                    contextReceiverGroup,
                    { it.toMemberScopeTowerLevel(emptyScopesCache, extensionReceiver = explicitReceiverValue) },
                    info, towerGroup, ExplicitReceiverKind.EXTENSION_RECEIVER,
                    emptyScopesCache,
                )
            }
        )
    }

    suspend fun runResolverForNoReceiver(
        info: CallInfo,
        emptyScopesCache: EmptyScopesCache,
    ) {
        processExtensionsThatHideMembers(info, explicitReceiverValue = null, emptyScopesCache = emptyScopesCache)

        enumerateTowerLevels(
            onScope = l@{ scope, group ->
                // NB: this check does not work for variables
                // because we do not search for objects if we have extension receiver
                if (info.callKind != CallKind.VariableAccess && scope in emptyScopesCache.emptyScopes) return@l

                processLevelWithoutCheck(
                    scope.toScopeTowerLevel(), info, group,
                    levelProducerAndCache = scope to emptyScopesCache.emptyScopes,
                )
            },
            onImplicitReceiver = { receiver, group ->
                processCandidatesWithGivenImplicitReceiverAsValue(
                    receiver,
                    info,
                    group,
                    emptyScopesCache,
                )
            },
            onContextReceiverGroup = { contextReceiverGroup, towerGroup ->
                processCandidatesWithGivenContextReceiverGroup(
                    contextReceiverGroup,
                    info, towerGroup, emptyScopesCache,
                )
            },
        )
    }

    suspend fun runResolverForSuperReceiver(
        info: CallInfo,
        superCall: FirQualifiedAccessExpression,
    ) {
        val receiverValue = ExpressionReceiverValue(superCall)
        processLevelWithoutCaching(
            receiverValue.toMemberScopeTowerLevel(),
            info, TowerGroup.Member, explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER,
        )
    }

    private suspend fun processExtensionsThatHideMembers(
        info: CallInfo,
        explicitReceiverValue: ReceiverValue?,
        parentGroup: TowerGroup = TowerGroup.EmptyRoot,
        emptyScopesCache: EmptyScopesCache,
    ) {
        // We will process hides members only for function calls with name in HIDES_MEMBERS_NAME_LIST
        if (info.callKind != CallKind.Function || info.name !in HIDES_MEMBERS_NAME_LIST) return

        val importingScopes = components.fileImportsScope.asReversed()
        for ((index, topLevelScope) in importingScopes.withIndex()) {
            if (explicitReceiverValue != null) {
                processHideMembersLevel(
                    explicitReceiverValue, topLevelScope, info, index, depth = null,
                    ExplicitReceiverKind.EXTENSION_RECEIVER, parentGroup,
                    emptyScopesCache,
                )
            } else {
                // context?
                for ((depth, implicitReceiverValue) in towerDataElementsForName.implicitReceivers) {
                    processHideMembersLevel(
                        implicitReceiverValue, topLevelScope, info, index, depth,
                        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, parentGroup,
                        emptyScopesCache,
                    )
                }
            }
        }
    }

    private suspend fun processScopeForExplicitReceiver(
        scope: FirScope,
        explicitReceiverValue: ExpressionReceiverValue,
        info: CallInfo,
        towerGroup: TowerGroup,
        emptyScopesCache: EmptyScopesCache,
    ) {
        processLevel(
            scope.toScopeTowerLevel(extensionReceiver = explicitReceiverValue),
            info, towerGroup,
            scope, emptyScopesCache.emptyScopes,
            ExplicitReceiverKind.EXTENSION_RECEIVER,
        )
    }

    private suspend fun processCandidatesWithGivenImplicitReceiverAsValue(
        receiver: ImplicitReceiverValue<*>,
        info: CallInfo,
        parentGroup: TowerGroup,
        emptyScopesCache: EmptyScopesCache
    ) {
        processLevel(
            receiver.toMemberScopeTowerLevel(), info, parentGroup.Member,
            levelProducer = receiver,
            cache = emptyScopesCache.implicitReceiverValuesWithEmptyScopes,
        )

        enumerateTowerLevels(
            parentGroup,
            onScope = l@{ scope, group ->
                processLevel(
                    scope.toScopeTowerLevel(extensionReceiver = receiver),
                    info, group,
                    levelProducer = scope,
                    cache = emptyScopesCache.emptyScopes,
                )
            },
            onImplicitReceiver = l@{ implicitReceiverValue, group ->
                processLevel(
                    implicitReceiverValue.toMemberScopeTowerLevel(
                        extensionReceiver = receiver,
                        givenExtensionReceiverCameFromImplicitReceiver = true,
                    ),
                    info, group,
                    levelProducer = implicitReceiverValue,
                    cache = emptyScopesCache.implicitReceiverValuesWithEmptyScopes,
                )
            },
            onContextReceiverGroup = { contextReceiverGroup, towerGroup ->
                processLevelForContextReceiverGroup(
                    contextReceiverGroup,
                    { it.toMemberScopeTowerLevel(emptyScopesCache, extensionReceiver = receiver) },
                    info, towerGroup,
                    emptyScopesCache = emptyScopesCache,
                )
            },
        )

    }

    private suspend fun processCandidatesWithGivenContextReceiverGroup(
        contextReceiverGroup: ContextReceiverGroup,
        info: CallInfo,
        parentGroup: TowerGroup,
        emptyScopesCache: EmptyScopesCache,
    ) {
        processLevelForContextReceiverGroup(
            contextReceiverGroup, { it.toMemberScopeTowerLevel(emptyScopesCache) }, info, parentGroup.Member,
            emptyScopesCache = emptyScopesCache,
        )

        enumerateTowerLevels(
            parentGroup,
            onScope = l@{ scope, towerGroup ->
                processLevel(
                    scope.toScopeTowerLevel(contextReceiverGroup = contextReceiverGroup),
                    info, towerGroup,
                    levelProducer = scope,
                    cache = emptyScopesCache.emptyScopes,
                )
            },
            onImplicitReceiver = { implicitReceiverValue, towerGroup ->
                processLevel(
                    implicitReceiverValue.toMemberScopeTowerLevel(contextReceiverGroup = contextReceiverGroup),
                    info, towerGroup,
                    levelProducer = implicitReceiverValue,
                    cache = emptyScopesCache.implicitReceiverValuesWithEmptyScopes,
                )
            },
            onContextReceiverGroup = { otherContextReceiverGroup, towerGroup ->
                processLevelForContextReceiverGroup(
                    contextReceiverGroup,
                    { it.toMemberScopeTowerLevel(emptyScopesCache, otherContextReceiverGroup = otherContextReceiverGroup) },
                    info, towerGroup,
                    emptyScopesCache = emptyScopesCache,
                )
            }
        )
    }

    private suspend fun processHideMembersLevel(
        receiverValue: ReceiverValue,
        topLevelScope: FirScope,
        info: CallInfo,
        index: Int,
        depth: Int?,
        explicitReceiverKind: ExplicitReceiverKind,
        parentGroup: TowerGroup,
        emptyScopesCache: EmptyScopesCache,
    ) {
        processLevel(
            topLevelScope.toScopeTowerLevel(
                extensionReceiver = receiverValue, withHideMembersOnly = true
            ),
            info,
            parentGroup.TopPrioritized(index).let { if (depth != null) it.Implicit(depth) else it },
            topLevelScope, emptyScopesCache.emptyScopes,
            explicitReceiverKind,
        )
    }
}
