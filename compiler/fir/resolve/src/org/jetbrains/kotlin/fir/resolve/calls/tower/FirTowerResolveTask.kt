/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.ExpressionReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.resolve.toImplicitResolvedQualifierReceiver
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
        companionExtensionPolicy: CompanionExtensionPolicy = CompanionExtensionPolicy.NoCompanionExtensions,
        dispatchReceiverForStatics: ExpressionReceiverValue? = null
    ): ScopeBasedTowerLevel {
        return ScopeBasedTowerLevel(
            components, this,
            givenExtensionReceiver = extensionReceiver?.receiverExpression,
            withHideMembersOnly, constructorFilter, companionExtensionPolicy, dispatchReceiverForStatics
        )
    }

    private fun ReceiverValue?.toConstructorFilter(): ConstructorFilter {
        return when (this) {
            null -> ConstructorFilter.OnlyNested
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
        dispatchReceiverForStatics = staticOwnerOwnerSymbol?.let {
            ExpressionReceiverValue(
                it.toImplicitResolvedQualifierReceiver(
                    components,
                    source?.fakeElement(KtFakeSourceElementKind.ImplicitReceiver)
                )
            )
        }
    )

    protected fun ReceiverValue.toDispatchReceiverMemberScopeTowerLevel(
        extensionReceiver: ReceiverValue? = null,
        skipSynthetics: Boolean = false,
    ) = DispatchReceiverMemberScopeTowerLevel(
        components, this,
        givenExtensionReceiver = extensionReceiver?.receiverExpression,
        skipSynthetics = skipSynthetics,
    )

    protected inline fun enumerateTowerLevels(
        parentGroup: TowerGroup = TowerGroup.EmptyRoot,
        onScope: (FirScope, FirRegularClassSymbol?, TowerGroup) -> Unit,
        onImplicitReceiver: (ImplicitReceiverValue<*>, TowerGroup) -> Unit,
        onStaticScopeOwnerSymbol: (FirRegularClassSymbol, TowerGroup) -> Unit = { _, _ -> },
    ) {
        for ((index, localScope) in towerDataElementsForName.reversedFilteredLocalScopes) {
            onScope(localScope, null, parentGroup.Local(index))
        }

        for ((depth, lexical) in towerDataElementsForName.nonLocalTowerDataElements.withIndex()) {
            if (!lexical.isLocal) {
                val scope = lexical.scope
                val staticScopeOwnerSymbol = lexical.staticScopeOwnerSymbol

                if (scope != null) {
                    onScope(scope, staticScopeOwnerSymbol, parentGroup.NonLocal(depth))
                }

                if (staticScopeOwnerSymbol != null) {
                    onStaticScopeOwnerSymbol(staticScopeOwnerSymbol, parentGroup.NonLocal(depth))
                }
            }

            val receiver = lexical.implicitReceiver

            if (receiver != null) {
                onImplicitReceiver(receiver, parentGroup.Implicit(depth))
            }
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

        enumerateTowerLevelsForCompanionExtensions(
            info,
            resolvedQualifier,
            TowerGroup.QualifierOrClassifier,
            explicitReceiverKind = ExplicitReceiverKind.EXTENSION_RECEIVER
        )

        if (resolvedQualifier.symbol != null) {
            if (info is CallableReferenceInfo && info.lhs is DoubleColonLHS.Type) {
                val stubReceiver = buildExpressionStub {
                    source = info.explicitReceiver?.source
                    this.coneTypeOrNull = info.lhs.type
                }

                val stubReceiverInfo = info.replaceExplicitReceiver(stubReceiver)

                runResolverForExpressionReceiver(stubReceiverInfo, stubReceiver, parentGroup = TowerGroup.QualifierValue)
            }

            // NB: canBeValue means it's resolved to an object or companion object
            if (resolvedQualifier.canBeValue && resolvedQualifier.typeArguments.isEmpty()) {
                runResolverForExpressionReceiver(info, resolvedQualifier, parentGroup = TowerGroup.QualifierValue)
            }

        }
    }

    private suspend fun enumerateTowerLevelsForCompanionExtensions(
        info: CallInfo,
        resolvedQualifier: FirResolvedQualifier,
        parentGroup: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind,
        emptyScopes: MutableSet<FirScope>? = null,
    ) {
        if (resolvedQualifier.symbol?.fullyExpandedClass(components.session)?.classKind.let { it == null || it == ClassKind.OBJECT }) {
            return
        }

        val explicitReceiverValue = ExpressionReceiverValue(resolvedQualifier)
        for ((depth, lexical) in towerDataElementsForName.nonLocalTowerDataElements.withIndex()) {
            val scope = lexical.scope

            if (!scope.canContainCompanionExtensions()) continue

            if (!lexical.isLocal && emptyScopes?.contains(scope) != true) {
                processScopeForExplicitReceiver(
                    scope,
                    explicitReceiverValue,
                    info,
                    parentGroup.NonLocal(depth),
                    companionExtensionPolicy = CompanionExtensionPolicy.OnlyCompanionExtensions,
                    explicitReceiverKind = explicitReceiverKind,
                    onEmptyLevel = { emptyScopes?.add(scope) }
                )
            }
        }
    }

    /**
     * Optimization: companion extensions can only be top-level, AND we know there are no companion extensions in the stdlib.
     */
    @OptIn(ExperimentalContracts::class)
    private fun FirScope?.canContainCompanionExtensions(): Boolean {
        contract { returns(true) implies (this@canContainCompanionExtensions != null) }
        return when (this) {
            is FirPackageMemberScope,
                // TODO(KT-84994) allow default importing scopes and optimize resolution differently.
            is FirExplicitSimpleImportingScope,
            is FirExplicitStarImportingScope,
                -> true
            else -> false
        }
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
                    group,
                    companionExtensionPolicy = CompanionExtensionPolicy.NoCompanionExtensions,
                )
            },
            onImplicitReceiver = { implicitReceiverValue, group ->
                // Member extensions
                processLevel(
                    implicitReceiverValue.toDispatchReceiverMemberScopeTowerLevel(extensionReceiver = explicitReceiverValue),
                    info, group.Member, ExplicitReceiverKind.EXTENSION_RECEIVER
                )
            }
        )
    }

    suspend fun runResolverForNoReceiver(
        info: CallInfo,
        skipSynthetics: Boolean = false,
    ) {
        val emptyScopes = mutableSetOf<FirScope>()
        val implicitReceiverValuesWithEmptyScopes = mutableSetOf<ImplicitReceiverValue<*>>()

        enumerateTowerLevels(
            onScope = { scope, staticScopeOwnerSymbol, group ->
                // NB: this check does not work for variables
                // because we do not search for objects if we have extension receiver
                if (info.callKind == CallKind.VariableAccess || scope !in emptyScopes) {
                    processLevel(
                        scope.toScopeBasedTowerLevelForStaticWithImplicitDispatchReceiver(
                            staticScopeOwnerSymbol, source = info.callSite.source
                        ),
                        info, group,
                        onEmptyLevel = {
                            emptyScopes += scope
                        }
                    )
                }
            },
            onStaticScopeOwnerSymbol = { staticScopeOwnerSymbol, group ->
                enumerateTowerLevelsForCompanionExtensionsForImplicitReceiver(info, staticScopeOwnerSymbol, group, emptyScopes)
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
        )
    }

    private suspend fun enumerateTowerLevelsForCompanionExtensionsForImplicitReceiver(
        info: CallInfo,
        staticScopeOwnerSymbol: FirRegularClassSymbol,
        group: TowerGroup,
        emptyScopes: MutableSet<FirScope>,
    ) {
        val receiver = staticScopeOwnerSymbol.toImplicitResolvedQualifierReceiver(
            components,
            info.callSite.source?.fakeElement(KtFakeSourceElementKind.ImplicitReceiver)
        )

        enumerateTowerLevelsForCompanionExtensions(
            info,
            receiver,
            group,
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            emptyScopes
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

    private suspend inline fun processScopeForExplicitReceiver(
        scope: FirScope,
        explicitReceiverValue: ExpressionReceiverValue,
        info: CallInfo,
        towerGroup: TowerGroup,
        companionExtensionPolicy: CompanionExtensionPolicy,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.EXTENSION_RECEIVER,
        onEmptyLevel: () -> Unit = {}
    ) {
        processLevel(
            scope.toScopeBasedTowerLevel(companionExtensionPolicy = companionExtensionPolicy, extensionReceiver = explicitReceiverValue),
            info, towerGroup, explicitReceiverKind,
            onEmptyLevel = onEmptyLevel,
        )
    }

    private suspend fun processCandidatesWithGivenImplicitReceiverAsValue(
        receiver: ImplicitReceiverValue<*>,
        info: CallInfo,
        parentGroup: TowerGroup,
        implicitReceiverValuesWithEmptyScopes: MutableSet<ImplicitReceiverValue<*>>,
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
