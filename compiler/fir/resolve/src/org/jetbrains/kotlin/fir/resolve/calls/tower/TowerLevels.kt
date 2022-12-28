/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.HidesMembers
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.runIf

enum class ProcessResult {
    FOUND, SCOPE_EMPTY;

    operator fun plus(other: ProcessResult): ProcessResult {
        if (this == FOUND || other == FOUND) return FOUND
        return this
    }
}

abstract class TowerScopeLevel {

    sealed class Token<out T : FirBasedSymbol<*>> {
        object Properties : Token<FirVariableSymbol<*>>()
        object Functions : Token<FirFunctionSymbol<*>>()
        object Objects : Token<FirBasedSymbol<*>>()
    }

    abstract fun processFunctionsByName(info: CallInfo, processor: TowerScopeLevelProcessor<FirFunctionSymbol<*>>): ProcessResult

    abstract fun processPropertiesByName(info: CallInfo, processor: TowerScopeLevelProcessor<FirVariableSymbol<*>>): ProcessResult

    abstract fun processObjectsByName(info: CallInfo, processor: TowerScopeLevelProcessor<FirBasedSymbol<*>>): ProcessResult

    interface TowerScopeLevelProcessor<in T : FirBasedSymbol<*>> {
        fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ReceiverValue?,
            importedQualifierForStatic: FirExpression?,
            givenExtensionReceiverOptions: List<ReceiverValue>,
            scope: FirScope,
            objectsByName: Boolean = false
        )
    }
}


// This is more like "dispatch receiver-based tower level"
// Here we always have an explicit or implicit dispatch receiver, and can access members of its scope
// (which is separated from currently accessible scope, see below)
// So: dispatch receiver = given explicit or implicit receiver (always present)
// So: extension receiver = either none, if dispatch receiver = explicit receiver,
//     or given implicit or explicit receiver, otherwise
class MemberScopeTowerLevel(
    private val bodyResolveComponents: BodyResolveComponents,
    val dispatchReceiverValue: ReceiverValue,
    private val givenExtensionReceiverOptions: List<ReceiverValue>,
) : TowerScopeLevel() {
    private val scopeSession: ScopeSession get() = bodyResolveComponents.scopeSession
    private val session: FirSession get() = bodyResolveComponents.session

    private fun <T : FirCallableSymbol<*>> processMembers(
        callInfo: CallInfo,
        output: TowerScopeLevelProcessor<T>,
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit
    ): ProcessResult {
        val scope = dispatchReceiverValue.scope(session, scopeSession) ?: return ProcessResult.SCOPE_EMPTY
        var (empty, candidates) = scope.collectCandidates(processScopeMembers)

        val scopeWithoutSmartcast = (dispatchReceiverValue.receiverExpression as? FirSmartCastExpression)
            ?.takeIf { it.isStable }
            ?.originalExpression?.typeRef
            ?.coneType
            ?.scope(
                session,
                scopeSession,
                bodyResolveComponents.returnTypeCalculator.fakeOverrideTypeCalculator,
                requiredPhase = FirResolvePhase.STATUS
            )
        if (scopeWithoutSmartcast == null) {
            consumeCandidates(output, candidates)
        } else {
            val candidatesFromOriginalType = mutableListOf<MemberWithBaseScope<T>>()
            scopeWithoutSmartcast.collectCandidates(processScopeMembers).let { (isEmpty, originalCandidates) ->
                empty = empty && isEmpty
                candidatesFromOriginalType += originalCandidates
            }
            if (candidatesFromOriginalType.isNotEmpty()) {
                processMembersFromSmartcastedType(callInfo, candidatesFromOriginalType, candidates, output)
            } else {
                consumeCandidates(output, candidates)
            }
        }

        if (givenExtensionReceiverOptions.isEmpty()) {
            val dispatchReceiverType = dispatchReceiverValue.type

            val useSiteForSyntheticScope: FirTypeScope
            val typeForSyntheticScope: ConeKotlinType

            // In K1, synthetic properties were working a bit differently
            // - On first step they've been built on the per-class level
            // - Then, they've been handled as regular extensions with specific receiver value
            // In K2, we build those properties using specific use-site scope of given receiver
            // And that gives us different results in case of raw types (since we've got special scopes for them)
            // So, here we decide to preserve the K1 behavior just by converting the type to its non-raw version
            if (dispatchReceiverType.isRaw()) {
                typeForSyntheticScope = dispatchReceiverType.convertToNonRawVersion()
                useSiteForSyntheticScope =
                    typeForSyntheticScope.scope(
                        session,
                        scopeSession,
                        FakeOverrideTypeCalculator.DoNothing,
                        requiredPhase = FirResolvePhase.STATUS
                    )
                        ?: error("No scope for flexible type scope, while it's not null for $dispatchReceiverType")
            } else {
                typeForSyntheticScope = dispatchReceiverType
                useSiteForSyntheticScope = scope
            }

            val withSynthetic = FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(
                session,
                typeForSyntheticScope,
                useSiteForSyntheticScope,
            )
            withSynthetic?.processScopeMembers { symbol ->
                empty = false
                output.consumeCandidate(
                    symbol,
                    dispatchReceiverValue,
                    importedQualifierForStatic = null,
                    givenExtensionReceiverOptions = emptyList(),
                    scope
                )
            }
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }

    private fun <T : FirCallableSymbol<*>> processMembersFromSmartcastedType(
        callInfo: CallInfo,
        candidatesFromOriginalType: Collection<MemberWithBaseScope<T>>,
        candidatesFromSmartcast: Collection<MemberWithBaseScope<T>>,
        output: TowerScopeLevelProcessor<T>,
    ) {
        val visibilityChecker = session.visibilityChecker
        val candidatesMapping = buildMap {
            candidatesFromOriginalType.forEach { put(it, false) }
            candidatesFromSmartcast.forEach { put(it, true) }
        }

        val overridableGroups = session.overrideService.createOverridableGroups(
            candidatesFromOriginalType + candidatesFromSmartcast,
            FirIntersectionScopeOverrideChecker(session)
        )

        val candidates = mutableListOf<MemberWithBaseScope<T>>()
        for (group in overridableGroups) {
            val visibleCandidates = group.filter {
                visibilityChecker.isVisible(it.member.fir, callInfo, dispatchReceiverValue, importedQualifierForStatic = null)
            }

            val visibleCandidatesFromSmartcast = visibleCandidates.filter { candidatesMapping.getValue(it) }
            candidates += visibleCandidatesFromSmartcast.ifEmpty { group }
        }
        consumeCandidates(output, candidates)
    }

    private fun <T : FirCallableSymbol<*>> FirTypeScope.collectCandidates(
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit
    ): Pair<Boolean, List<MemberWithBaseScope<T>>> {
        var empty = true
        val result = mutableListOf<MemberWithBaseScope<T>>()
        processScopeMembers { candidate ->
            empty = false
            if (candidate.hasConsistentExtensionReceiver(givenExtensionReceiverOptions)) {
                val fir = candidate.fir
                if ((fir as? FirConstructor)?.isInner == false) {
                    return@processScopeMembers
                }
                result += MemberWithBaseScope(candidate, this)
            }
        }
        return empty to result
    }

    private fun <T : FirCallableSymbol<*>> consumeCandidates(
        output: TowerScopeLevelProcessor<T>,
        candidatesWithScope: List<MemberWithBaseScope<T>>
    ) {
        for ((candidate, scope) in candidatesWithScope) {
            if (candidate.hasConsistentExtensionReceiver(givenExtensionReceiverOptions)) {
                output.consumeCandidate(
                    candidate,
                    dispatchReceiverValue,
                    importedQualifierForStatic = null,
                    givenExtensionReceiverOptions,
                    scope
                )
            }
        }
    }

    override fun processFunctionsByName(
        info: CallInfo,
        processor: TowerScopeLevelProcessor<FirFunctionSymbol<*>>
    ): ProcessResult {
        val lookupTracker = session.lookupTracker
        return processMembers(info, processor) { consumer ->
            withMemberCallLookup(lookupTracker, info) { lookupCtx ->
                this.processFunctionsAndConstructorsByName(
                    info, session, bodyResolveComponents,
                    includeInnerConstructors = true,
                    processor = {
                        lookupCtx.recordCallableMemberLookup(it)
                        // WARNING, DO NOT CAST FUNCTIONAL TYPE ITSELF
                        consumer(it as FirFunctionSymbol<*>)
                    }
                )
            }
        }
    }

    override fun processPropertiesByName(
        info: CallInfo,
        processor: TowerScopeLevelProcessor<FirVariableSymbol<*>>
    ): ProcessResult {
        val lookupTracker = session.lookupTracker
        return processMembers(info, processor) { consumer ->
            withMemberCallLookup(lookupTracker, info) { lookupCtx ->
                lookupTracker?.recordCallLookup(info, dispatchReceiverValue.type)
                this.processPropertiesByName(info.name) {
                    lookupCtx.recordCallableMemberLookup(it)
                    consumer(it)
                }
            }
        }
    }

    override fun processObjectsByName(
        info: CallInfo,
        processor: TowerScopeLevelProcessor<FirBasedSymbol<*>>
    ): ProcessResult {
        return ProcessResult.FOUND
    }

    private inline fun withMemberCallLookup(
        lookupTracker: FirLookupTrackerComponent?,
        info: CallInfo,
        body: (Triple<FirLookupTrackerComponent?, SmartList<String>, CallInfo>) -> Unit
    ) {
        lookupTracker?.recordCallLookup(info, dispatchReceiverValue.type)
        val lookupScopes = SmartList<String>()
        body(Triple(lookupTracker, lookupScopes, info))
        if (lookupScopes.isNotEmpty()) {
            lookupTracker?.recordCallLookup(info, lookupScopes)
        }
    }

    private fun Triple<FirLookupTrackerComponent?, SmartList<String>, CallInfo>.recordCallableMemberLookup(callable: FirCallableSymbol<*>) {
        first?.run {
            recordTypeResolveAsLookup(callable.fir.returnTypeRef, third.callSite.source, third.containingFile.source)
            callable.callableId.className?.let { lookupScope ->
                second.add(lookupScope.asString())
            }
        }
    }

    private fun FirCallableSymbol<*>.hasConsistentExtensionReceiver(givenExtensionReceivers: List<ReceiverValue>): Boolean {
        return givenExtensionReceivers.isNotEmpty() == hasExtensionReceiver()
    }
}

class ContextReceiverGroupMemberScopeTowerLevel(
    bodyResolveComponents: BodyResolveComponents,
    contextReceiverGroup: ContextReceiverGroup,
    givenExtensionReceiverOptions: List<ReceiverValue> = emptyList(),
) : TowerScopeLevel() {
    private val memberScopeLevels = contextReceiverGroup.map {
        MemberScopeTowerLevel(bodyResolveComponents, it, givenExtensionReceiverOptions)
    }

    override fun processFunctionsByName(info: CallInfo, processor: TowerScopeLevelProcessor<FirFunctionSymbol<*>>): ProcessResult {
        return memberScopeLevels.minOf { it.processFunctionsByName(info, processor) }
    }

    override fun processPropertiesByName(info: CallInfo, processor: TowerScopeLevelProcessor<FirVariableSymbol<*>>): ProcessResult {
        return memberScopeLevels.minOf { it.processPropertiesByName(info, processor) }
    }

    override fun processObjectsByName(info: CallInfo, processor: TowerScopeLevelProcessor<FirBasedSymbol<*>>): ProcessResult {
        return memberScopeLevels.minOf { it.processObjectsByName(info, processor) }
    }
}

// This is more like "scope-based tower level"
// We can access here members of currently accessible scope which is not influenced by explicit receiver
// We can either have no explicit receiver at all, or it can be an extension receiver
// An explicit receiver never can be a dispatch receiver at this level
// So: dispatch receiver = strictly none (EXCEPTIONS: importing scopes with import from objects, synthetic field variable)
// So: extension receiver = either none or explicit
// (if explicit receiver exists, it always *should* be an extension receiver)
class ScopeTowerLevel(
    private val bodyResolveComponents: BodyResolveComponents,
    val scope: FirScope,
    private val givenExtensionReceiverOptions: List<ReceiverValue>,
    private val withHideMembersOnly: Boolean,
    private val includeInnerConstructors: Boolean
) : TowerScopeLevel() {
    private val session: FirSession get() = bodyResolveComponents.session

    fun areThereExtensionReceiverOptions(): Boolean = givenExtensionReceiverOptions.isNotEmpty()

    private fun FirCallableDeclaration.importedQualifierForObjectOrStatic(): FirExpression? {
        val objectClassId = importedFromObjectOrStaticData?.objectClassId ?: return null
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(objectClassId)
        if (symbol is FirRegularClassSymbol) {
            return buildResolvedQualifier {
                packageFqName = objectClassId.packageFqName
                relativeClassFqName = objectClassId.relativeClassName
                this.symbol = symbol
            }.apply {
                resultType = bodyResolveComponents.typeForQualifier(this)
            }
        }
        return null
    }

    private fun dispatchReceiverValue(candidate: FirCallableSymbol<*>): ReceiverValue? {
        if (candidate.fir.isStatic) return null
        val importedQualifierReceiver = candidate.fir.importedQualifierForObjectOrStatic()
        if (importedQualifierReceiver != null) {
            return ExpressionReceiverValue(importedQualifierReceiver)
        }

        if (candidate !is FirBackingFieldSymbol) {
            return null
        }

        val lookupTag = candidate.fir.propertySymbol.dispatchReceiverClassLookupTagOrNull()
        return when {
            lookupTag != null -> {
                bodyResolveComponents.implicitReceiverStack.lastDispatchReceiver { implicitReceiverValue ->
                    (implicitReceiverValue.type as? ConeClassLikeType)?.fullyExpandedType(session)?.lookupTag == lookupTag
                }
            }

            else -> {
                bodyResolveComponents.implicitReceiverStack.lastDispatchReceiver()
            }
        }
    }

    private fun shouldSkipCandidateWithInconsistentExtensionReceiver(candidate: FirCallableSymbol<*>): Boolean {
        // Pre-check explicit extension receiver for default package top-level members
        if (scope !is FirDefaultStarImportingScope || !areThereExtensionReceiverOptions()) return false

        val declarationReceiverType = candidate.resolvedReceiverTypeRef?.coneType as? ConeClassLikeType ?: return false
        val startProjectedDeclarationReceiverType = declarationReceiverType.lookupTag.constructClassType(
            declarationReceiverType.typeArguments.map { ConeStarProjection }.toTypedArray(),
            isNullable = true
        )

        return givenExtensionReceiverOptions.none { extensionReceiver ->
            val extensionReceiverType = extensionReceiver.type
            // If some receiver is non class like, we should not skip it
            if (extensionReceiverType !is ConeClassLikeType) return@none true

            AbstractTypeChecker.isSubtypeOf(
                session.typeContext,
                extensionReceiverType,
                startProjectedDeclarationReceiverType
            )
        }
    }

    private fun <T : FirBasedSymbol<*>> consumeCallableCandidate(
        candidate: FirCallableSymbol<*>,
        processor: TowerScopeLevelProcessor<T>
    ) {
        val candidateReceiverTypeRef = candidate.fir.receiverParameter?.typeRef
        if (withHideMembersOnly && candidate.getAnnotationByClassId(HidesMembers, session) == null) {
            return
        }
        val receiverExpected = withHideMembersOnly || areThereExtensionReceiverOptions()
        if (candidateReceiverTypeRef == null == receiverExpected) return
        val dispatchReceiverValue = dispatchReceiverValue(candidate)
        val isStatic = candidate.fir.isStatic
        val importedQualifierForStatic = runIf(isStatic) {
            candidate.fir.importedQualifierForObjectOrStatic()
        }
        if (dispatchReceiverValue == null &&
            importedQualifierForStatic == null &&
            shouldSkipCandidateWithInconsistentExtensionReceiver(candidate)
        ) {
            return
        }
        val unwrappedCandidate = candidate.fir.importedFromObjectOrStaticData?.original?.symbol.takeIf { !isStatic } ?: candidate
        @Suppress("UNCHECKED_CAST")
        processor.consumeCandidate(
            unwrappedCandidate as T,
            dispatchReceiverValue,
            importedQualifierForStatic,
            givenExtensionReceiverOptions,
            scope
        )
    }

    override fun processFunctionsByName(
        info: CallInfo,
        processor: TowerScopeLevelProcessor<FirFunctionSymbol<*>>
    ): ProcessResult {
        var empty = true
        session.lookupTracker?.recordCallLookup(info, scope.scopeOwnerLookupNames)
        scope.processFunctionsAndConstructorsByName(
            info,
            session,
            bodyResolveComponents,
            includeInnerConstructors = includeInnerConstructors
        ) { candidate ->
            empty = false
            consumeCallableCandidate(candidate, processor)
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }

    override fun processPropertiesByName(
        info: CallInfo,
        processor: TowerScopeLevelProcessor<FirVariableSymbol<*>>
    ): ProcessResult {
        var empty = true
        session.lookupTracker?.recordCallLookup(info, scope.scopeOwnerLookupNames)
        scope.processPropertiesByName(info.name) { candidate ->
            empty = false
            consumeCallableCandidate(candidate, processor)
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }

    override fun processObjectsByName(
        info: CallInfo,
        processor: TowerScopeLevelProcessor<FirBasedSymbol<*>>
    ): ProcessResult {
        var empty = true
        session.lookupTracker?.recordCallLookup(info, scope.scopeOwnerLookupNames)
        scope.processClassifiersByName(info.name) {
            empty = false
            processor.consumeCandidate(
                it,
                dispatchReceiverValue = null,
                importedQualifierForStatic = null,
                givenExtensionReceiverOptions = emptyList(),
                scope = scope,
                objectsByName = true
            )
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }
}

private fun FirCallableSymbol<*>.hasExtensionReceiver(): Boolean {
    return fir.receiverParameter != null
}
