/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.ContextReceiverGroup
import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.resolve.setTypeOfQualifier
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST

internal class TowerDataElementsForName(
    name: Name,
    towerDataContext: FirTowerDataContext
) {
    val nonLocalTowerDataElements = towerDataContext.nonLocalTowerDataElements.asReversed()

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
            // Context receiver group can be not-null but empty (if we have a non-empty context parameter group).
            // In that case we want to return null.
            towerDataElement.contextReceiverGroup?.takeUnless { it.isEmpty() }?.let { receiver -> IndexedValue(index, receiver) }
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

    protected suspend inline fun processLevel(
        towerLevel: TowerLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        onEmptyLevel: () -> Unit = {}
    ) {
        if (processLevel(towerLevel, callInfo, group, explicitReceiverKind)) {
            onEmptyLevel()
        }
    }

    protected fun FirScope.toScopeBasedTowerLevel(
        extensionReceiver: ReceiverValue? = null,
        withHideMembersOnly: Boolean = false,
        constructorFilter: ConstructorFilter = extensionReceiver.toConstructorFilter(),
        contextReceiverGroup: ContextReceiverGroup? = null,
        dispatchReceiverForStatics: ExpressionReceiverValue? = null
    ): ScopeBasedTowerLevel {
        return ScopeBasedTowerLevel(
            components, this,
            givenExtensionReceiverOptions = createExtensionReceiverOptions(contextReceiverGroup, extensionReceiver),
            withHideMembersOnly, constructorFilter, dispatchReceiverForStatics
        )
    }

    private fun ReceiverValue?.toConstructorFilter(): ConstructorFilter {
        return when (this) {
            null -> ConstructorFilter.OnlyNested
            is PhantomStaticThis -> ConstructorFilter.OnlyNested
            else -> ConstructorFilter.Both
        }
    }

    protected fun FirScope.toScopeBasedTowerLevelForStaticWithImplicitDispatchReceiver(
        staticOwnerOwnerSymbol: FirRegularClassSymbol? = null,
        source: KtSourceElement? = null
    ): ScopeBasedTowerLevel = toScopeBasedTowerLevel(
        extensionReceiver = null,
        withHideMembersOnly = false,
        constructorFilter = ConstructorFilter.OnlyNested,
        contextReceiverGroup = null,
        staticOwnerOwnerSymbol?.let {
            val resolvedQualifier = buildResolvedQualifier {
                packageFqName = it.classId.packageFqName
                relativeClassFqName = it.classId.relativeClassName
                this.symbol = it
                this.source = source?.fakeElement(KtFakeSourceElementKind.ImplicitReceiver)
            }.apply {
                setTypeOfQualifier(components)
            }
            ExpressionReceiverValue(resolvedQualifier)
        }
    )

    protected fun ReceiverValue.toDispatchReceiverMemberScopeTowerLevel(
        extensionReceiver: ReceiverValue? = null,
        contextReceiverGroup: ContextReceiverGroup? = null,
        skipSynthetics: Boolean = false,
    ) = DispatchReceiverMemberScopeTowerLevel(
        components, this,
        givenExtensionReceiverOptions = createExtensionReceiverOptions(contextReceiverGroup, extensionReceiver),
        skipSynthetics = skipSynthetics,
    )

    protected fun ContextReceiverGroup.toDispatchReceiverMemberScopeTowerLevel(
        extensionReceiver: ReceiverValue? = null,
        otherContextReceiverGroup: ContextReceiverGroup? = null,
    ) = ContextReceiverGroupMemberScopeTowerLevel(
        components, this,
        givenExtensionReceiverOptions = createExtensionReceiverOptions(otherContextReceiverGroup, extensionReceiver),
    )

    private fun createExtensionReceiverOptions(
        contextReceiverGroup: ContextReceiverGroup?,
        extensionReceiverValue: ReceiverValue?,
    ): List<FirExpression> {
        return when {
            contextReceiverGroup != null -> contextReceiverGroup.map { it.receiverExpression }
            extensionReceiverValue != null -> listOf(extensionReceiverValue.receiverExpression)
            else -> emptyList()
        }
    }

    protected inline fun enumerateTowerLevels(
        parentGroup: TowerGroup = TowerGroup.EmptyRoot,
        onScope: (FirScope, FirRegularClassSymbol?, TowerGroup) -> Unit,
        onImplicitReceiver: (ImplicitReceiver<*>, TowerGroup) -> Unit,
        onContextReceiverGroup: (ContextReceiverGroup, TowerGroup) -> Unit,
    ) {
        for ((index, localScope) in towerDataElementsForName.reversedFilteredLocalScopes) {
            onScope(localScope, null, parentGroup.Local(index))
        }

        for ((depth, lexical) in towerDataElementsForName.nonLocalTowerDataElements.withIndex()) {
            val scope = lexical.scope
            if (!lexical.isLocal && scope != null) {
                onScope(
                    scope,
                    lexical.staticScopeOwnerSymbol,
                    parentGroup.NonLocal(depth)
                )
            }

            val receiver = lexical.implicitReceiver

            if (receiver != null) {
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
        towerLevel: TowerLevel,
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
        resolvedQualifier: FirResolvedQualifier
    ) {
        val qualifierReceiver = createQualifierReceiver(resolvedQualifier, session, components.scopeSession)

        processQualifierScopes(info, qualifierReceiver)
        processClassifierScope(info, qualifierReceiver)

        if (resolvedQualifier.symbol != null && info is CallableReferenceInfo && info.lhs is DoubleColonLHS.Type) {
            val stubReceiver = buildExpressionStub {
                source = info.explicitReceiver?.source
                this.coneTypeOrNull = info.lhs.type
            }
            val stubReceiverInfo = info.replaceExplicitReceiver(stubReceiver)
            runResolverForExpressionReceiver(stubReceiverInfo, stubReceiver, parentGroup = TowerGroup.QualifierValue)
        }

        processMixedStaticAndCompanionObjectScope(info, qualifierReceiver, resolvedQualifier)
    }

    private suspend fun processQualifierScopes(
        info: CallInfo, qualifierReceiver: QualifierReceiver?
    ) {
        if (qualifierReceiver == null) return
        val callableScope = qualifierReceiver.callableScope() ?: return
        processLevel(
            callableScope.toScopeBasedTowerLevel(
                constructorFilter = ConstructorFilter.OnlyNested,
                dispatchReceiverForStatics = when (qualifierReceiver) {
                    is ClassQualifierReceiver -> ExpressionReceiverValue(qualifierReceiver.explicitReceiver)
                    else -> null
                }
            ),
            info, TowerGroup.QualifierOrClassifier
        )
    }

    private suspend fun processClassifierScope(
        info: CallInfo, qualifierReceiver: QualifierReceiver?
    ) {
        if (qualifierReceiver == null) return
        if (info.callKind != CallKind.CallableReference &&
            qualifierReceiver is ClassQualifierReceiver &&
            qualifierReceiver.classSymbol != qualifierReceiver.originalSymbol
        ) return
        val scope = qualifierReceiver.classifierScope() ?: return
        processLevel(
            scope.toScopeBasedTowerLevel(constructorFilter = ConstructorFilter.OnlyNested), info,
            TowerGroup.QualifierOrClassifier
        )
    }

    private suspend fun processMixedStaticAndCompanionObjectScope(
        info: CallInfo, qualifierReceiver: QualifierReceiver?, resolvedQualifier: FirResolvedQualifier
    ) {
        val explicitCompanionReceiverValue =
            if (resolvedQualifier.symbol != null && resolvedQualifier.canBeValue && resolvedQualifier.typeArguments.isEmpty())
                ExpressionReceiverValue(resolvedQualifier)
            else null

        if (explicitCompanionReceiverValue != null) {
            processExtensionsThatHideMembers(
                info, explicitCompanionReceiverValue, TowerGroup.QualifierValue,
                ExplicitReceiverKind.EXTENSION_RECEIVER,
            )
            processLevel(
                explicitCompanionReceiverValue.toDispatchReceiverMemberScopeTowerLevel(), info,
                TowerGroup.QualifierValue.Member, ExplicitReceiverKind.DISPATCH_RECEIVER
            )
        }

        val phantomReceiver =
            if (qualifierReceiver is ClassQualifierReceiver && !qualifierReceiver.classSymbol.classKind.isSingleton)
                PhantomStaticThis(qualifierReceiver.originalSymbol, components.session, components.scopeSession)
            else null

        val allReceiverValues = listOfNotNull(phantomReceiver, explicitCompanionReceiverValue)

        enumerateTowerLevels(
            parentGroup = TowerGroup.QualifierValue,
            onScope = { scope, _, group ->
                for (receiverValue in allReceiverValues) {
                    processScopeForExplicitReceiver(
                        scope, receiverValue, info, group
                    )
                }
            },
            onImplicitReceiver = { implicitReceiverValue, group ->
                for (receiverValue in allReceiverValues) {
                    processLevel(
                        implicitReceiverValue.toDispatchReceiverMemberScopeTowerLevel(extensionReceiver = receiverValue),
                        info, group.Member, ExplicitReceiverKind.EXTENSION_RECEIVER
                    )
                }
            },
            onContextReceiverGroup = { contextReceiverGroup, towerGroup ->
                for (receiverValue in allReceiverValues) {
                    processLevel(
                        contextReceiverGroup.toDispatchReceiverMemberScopeTowerLevel(extensionReceiver = receiverValue),
                        info, towerGroup, ExplicitReceiverKind.EXTENSION_RECEIVER,
                    )
                }
            }
        )
    }

    suspend fun runResolverForExpressionReceiver(
        info: CallInfo,
        receiver: FirExpression,
        parentGroup: TowerGroup = TowerGroup.EmptyRoot
    ) {
        val explicitReceiverValue = ExpressionReceiverValue(receiver)

        processExtensionsThatHideMembers(
            info, explicitReceiverValue, parentGroup,
            ExplicitReceiverKind.EXTENSION_RECEIVER,
        )

        // Member scope of expression receiver
        processLevel(
            explicitReceiverValue.toDispatchReceiverMemberScopeTowerLevel(), info,
            parentGroup.Member, ExplicitReceiverKind.DISPATCH_RECEIVER
        )

        enumerateTowerLevels(
            parentGroup = parentGroup,
            onScope = { scope, _, group ->
                processScopeForExplicitReceiver(
                    scope,
                    explicitReceiverValue,
                    info,
                    group
                )
            },
            onImplicitReceiver = { implicitReceiverValue, group ->
                // Member extensions
                processLevel(
                    implicitReceiverValue.toDispatchReceiverMemberScopeTowerLevel(extensionReceiver = explicitReceiverValue),
                    info, group.Member, ExplicitReceiverKind.EXTENSION_RECEIVER
                )
            },
            onContextReceiverGroup = { contextReceiverGroup, towerGroup ->
                processLevel(
                    contextReceiverGroup.toDispatchReceiverMemberScopeTowerLevel(extensionReceiver = explicitReceiverValue),
                    info, towerGroup, ExplicitReceiverKind.EXTENSION_RECEIVER,
                )
            }
        )
    }

    suspend fun runResolverForNoReceiver(
        info: CallInfo,
        skipSynthetics: Boolean = false,
    ) {
        val emptyScopes = mutableSetOf<FirScope>()
        val implicitReceiverValuesWithEmptyScopes = mutableSetOf<ImplicitReceiver<*>>()

        enumerateTowerLevels(
            onScope = l@{ scope, staticScopeOwnerSymbol, group ->
                // NB: this check does not work for variables
                // because we do not search for objects if we have extension receiver
                if (info.callKind != CallKind.VariableAccess && scope in emptyScopes) return@l

                processLevel(
                    scope.toScopeBasedTowerLevelForStaticWithImplicitDispatchReceiver(
                        staticScopeOwnerSymbol, source = info.callSite.source
                    ),
                    info, group,
                    onEmptyLevel = {
                        emptyScopes += scope
                    }
                )
            },
            onImplicitReceiver = { receiver, group ->
                processCandidatesWithGivenImplicitReceiverAsValue(
                    receiver,
                    info,
                    group,
                    implicitReceiverValuesWithEmptyScopes,
                    emptyScopes,
                    skipSynthetics,
                )
            },
            onContextReceiverGroup = { contextReceiverGroup, towerGroup ->
                processCandidatesWithGivenContextReceiverGroup(
                    contextReceiverGroup,
                    info, towerGroup,
                )
            },
        )
    }

    suspend fun runResolverForSuperReceiver(
        info: CallInfo,
        superCall: FirQualifiedAccessExpression,
    ) {
        val receiverValue = ExpressionReceiverValue(superCall)
        processLevel(
            receiverValue.toDispatchReceiverMemberScopeTowerLevel(),
            info, TowerGroup.Member, explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER,
        )
    }

    private suspend fun processExtensionsThatHideMembers(
        info: CallInfo,
        receiverValue: ReceiverValue,
        parentGroup: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind,
    ) {
        // We will process hides members only for function calls with name in HIDES_MEMBERS_NAME_LIST
        if (info.callKind != CallKind.Function || info.name !in HIDES_MEMBERS_NAME_LIST) return

        val importingScopes = components.fileImportsScope.asReversed()
        for ((index, topLevelScope) in importingScopes.withIndex()) {
            processHideMembersLevel(
                receiverValue, topLevelScope, info, index,
                explicitReceiverKind, parentGroup
            )
        }
    }

    private suspend fun processScopeForExplicitReceiver(
        scope: FirScope,
        explicitReceiverValue: ReceiverValue,
        info: CallInfo,
        towerGroup: TowerGroup,
    ) {
        processLevel(
            scope.toScopeBasedTowerLevel(extensionReceiver = explicitReceiverValue),
            info, towerGroup, ExplicitReceiverKind.EXTENSION_RECEIVER
        )

    }

    private suspend fun processCandidatesWithGivenImplicitReceiverAsValue(
        receiver: ImplicitReceiver<*>,
        info: CallInfo,
        parentGroup: TowerGroup,
        implicitReceiverValuesWithEmptyScopes: MutableSet<ImplicitReceiver<*>>,
        emptyScopes: MutableSet<FirScope>,
        skipSynthetics: Boolean,
    ) {
        processExtensionsThatHideMembers(
            info, receiver, parentGroup,
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        )

        processLevel(
            receiver.toDispatchReceiverMemberScopeTowerLevel(skipSynthetics = skipSynthetics), info, parentGroup.Member,
            onEmptyLevel = {
                implicitReceiverValuesWithEmptyScopes += receiver
            }
        )

        enumerateTowerLevels(
            parentGroup,
            onScope = l@{ scope, _, group ->
                if (scope in emptyScopes) return@l

                processLevel(
                    scope.toScopeBasedTowerLevel(extensionReceiver = receiver),
                    info, group,
                    onEmptyLevel = {
                        emptyScopes += scope
                    }
                )
            },
            onImplicitReceiver = l@{ implicitReceiverValue, group ->
                if (implicitReceiverValue in implicitReceiverValuesWithEmptyScopes) return@l
                processLevel(
                    implicitReceiverValue.toDispatchReceiverMemberScopeTowerLevel(extensionReceiver = receiver),
                    info, group
                )
            },
            onContextReceiverGroup = { contextReceiverGroup, towerGroup ->
                processLevel(
                    contextReceiverGroup.toDispatchReceiverMemberScopeTowerLevel(extensionReceiver = receiver),
                    info, towerGroup
                )
            },
        )

    }

    private suspend fun processCandidatesWithGivenContextReceiverGroup(
        contextReceiverGroup: ContextReceiverGroup,
        info: CallInfo,
        parentGroup: TowerGroup,
    ) {
        processLevel(
            contextReceiverGroup.toDispatchReceiverMemberScopeTowerLevel(), info, parentGroup.Member,
        )

        enumerateTowerLevels(
            parentGroup,
            onScope = { scope, _, towerGroup ->
                processLevel(
                    scope.toScopeBasedTowerLevel(contextReceiverGroup = contextReceiverGroup),
                    info, towerGroup,
                )
            },
            onImplicitReceiver = { implicitReceiverValue, towerGroup ->
                processLevel(
                    implicitReceiverValue.toDispatchReceiverMemberScopeTowerLevel(contextReceiverGroup = contextReceiverGroup),
                    info, towerGroup
                )
            },
            onContextReceiverGroup = { otherContextReceiverGroup, towerGroup ->
                processLevel(
                    contextReceiverGroup.toDispatchReceiverMemberScopeTowerLevel(otherContextReceiverGroup = otherContextReceiverGroup),
                    info, towerGroup,
                )
            }
        )
    }

    private suspend fun processHideMembersLevel(
        receiverValue: ReceiverValue,
        topLevelScope: FirScope,
        info: CallInfo,
        index: Int,
        explicitReceiverKind: ExplicitReceiverKind,
        parentGroup: TowerGroup
    ) {
        processLevel(
            topLevelScope.toScopeBasedTowerLevel(
                extensionReceiver = receiverValue, withHideMembersOnly = true
            ),
            info,
            parentGroup.TopPrioritized(index),
            explicitReceiverKind,
        )
    }
}
