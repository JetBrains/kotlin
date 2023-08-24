/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.ContextReceiverGroup
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.HidesMembers
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

enum class ProcessResult {
    FOUND, SCOPE_EMPTY;

    operator fun plus(other: ProcessResult): ProcessResult {
        if (this == FOUND || other == FOUND) return FOUND
        return this
    }
}

abstract class TowerScopeLevel {
    abstract fun processFunctionsByName(info: CallInfo, processor: TowerScopeLevelProcessor<FirFunctionSymbol<*>>): ProcessResult

    abstract fun processPropertiesByName(info: CallInfo, processor: TowerScopeLevelProcessor<FirVariableSymbol<*>>): ProcessResult

    abstract fun processObjectsByName(info: CallInfo, processor: TowerScopeLevelProcessor<FirBasedSymbol<*>>): ProcessResult

    interface TowerScopeLevelProcessor<in T : FirBasedSymbol<*>> {
        fun consumeCandidate(
            symbol: T,
            dispatchReceiver: FirExpression?,
            givenExtensionReceiverOptions: List<FirExpression>,
            scope: FirScope,
            objectsByName: Boolean = false,
            isFromOriginalTypeInPresenceOfSmartCast: Boolean = false,
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
    private val givenExtensionReceiverOptions: List<FirExpression>,
    private val skipSynthetics: Boolean,
) : TowerScopeLevel() {
    private val scopeSession: ScopeSession get() = bodyResolveComponents.scopeSession
    private val session: FirSession get() = bodyResolveComponents.session

    private fun <T : FirCallableSymbol<*>> processMembers(
        output: TowerScopeLevelProcessor<T>,
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit
    ): ProcessResult {
        val scope = dispatchReceiverValue.scope(session, scopeSession) ?: return ProcessResult.SCOPE_EMPTY
        var (empty, candidates) = scope.collectCandidates(processScopeMembers)

        val scopeWithoutSmartcast = getOriginalReceiverExpressionIfStableSmartCast()
            ?.resolvedType
            ?.scope(
                session,
                scopeSession,
                bodyResolveComponents.returnTypeCalculator.fakeOverrideTypeCalculator,
                requiredMembersPhase = FirResolvePhase.STATUS,
            )

        if (scopeWithoutSmartcast == null) {
            consumeCandidates(output, candidates)
        } else {
            val isFromSmartCast: MutableMap<MemberWithBaseScope<T>, Boolean> = mutableMapOf()

            scopeWithoutSmartcast.collectCandidates(processScopeMembers).let { (isEmpty, originalCandidates) ->
                empty = empty && isEmpty
                for (originalCandidate in originalCandidates) {
                    isFromSmartCast[originalCandidate] = false
                }
            }

            for (candidateFromSmartCast in candidates) {
                isFromSmartCast[candidateFromSmartCast] = true
            }

            consumeCandidates(
                output,
                // all the candidates, both from original type and smart cast
                candidates = isFromSmartCast.keys,
                isFromSmartCast,
            )
        }

        if (givenExtensionReceiverOptions.isEmpty() && !skipSynthetics) {
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
                useSiteForSyntheticScope = typeForSyntheticScope.scope(
                    session,
                    scopeSession,
                    FakeOverrideTypeCalculator.DoNothing,
                    requiredMembersPhase = FirResolvePhase.STATUS,
                ) ?: errorWithAttachment("No scope for flexible type scope, while it's not null") {
                    withConeTypeEntry("dispatchReceiverType", dispatchReceiverType)
                }
            } else {
                typeForSyntheticScope = dispatchReceiverType
                useSiteForSyntheticScope = scope
            }

            val withSynthetic = FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(
                session,
                typeForSyntheticScope,
                useSiteForSyntheticScope,
                bodyResolveComponents.returnTypeCalculator,
            )

            withSynthetic?.processScopeMembers { symbol ->
                empty = false
                output.consumeCandidate(
                    symbol,
                    dispatchReceiverValue.receiverExpression,
                    givenExtensionReceiverOptions = emptyList(),
                    scope
                )
            }
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
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
        candidates: Collection<MemberWithBaseScope<T>>,
        // The map is not null only if there's a smart cast type on a dispatch receiver
        // and candidates are present both in smart cast and original types.
        // isFromSmartCast[candidate] == true iff exactly that member is present in smart cast type
        isFromSmartCast: Map<MemberWithBaseScope<T>, Boolean>? = null
    ) {
        for (candidateWithScope in candidates) {
            val (candidate, scope) = candidateWithScope
            if (candidate.hasConsistentExtensionReceiver(givenExtensionReceiverOptions)) {
                val isFromOriginalTypeInPresenceOfSmartCast = isFromSmartCast != null && !isFromSmartCast.getValue(candidateWithScope)

                val dispatchReceiverToUse = when {
                    isFromOriginalTypeInPresenceOfSmartCast ->
                        getOriginalReceiverExpressionIfStableSmartCast()
                    else -> dispatchReceiverValue.receiverExpression
                }

                output.consumeCandidate(
                    candidate,
                    dispatchReceiverToUse,
                    givenExtensionReceiverOptions,
                    scope,
                    isFromOriginalTypeInPresenceOfSmartCast = isFromOriginalTypeInPresenceOfSmartCast
                )
            }
        }
    }

    private fun getOriginalReceiverExpressionIfStableSmartCast() =
        (dispatchReceiverValue.receiverExpression as? FirSmartCastExpression)
            ?.takeIf { it.isStable }
            ?.originalExpression

    override fun processFunctionsByName(
        info: CallInfo,
        processor: TowerScopeLevelProcessor<FirFunctionSymbol<*>>
    ): ProcessResult {
        val lookupTracker = session.lookupTracker
        return processMembers(processor) { consumer ->
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
        return processMembers(processor) { consumer ->
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

    private fun FirCallableSymbol<*>.hasConsistentExtensionReceiver(givenExtensionReceivers: List<FirExpression>): Boolean {
        return givenExtensionReceivers.isNotEmpty() == hasExtensionReceiver()
    }
}

class ContextReceiverGroupMemberScopeTowerLevel(
    bodyResolveComponents: BodyResolveComponents,
    contextReceiverGroup: ContextReceiverGroup,
    givenExtensionReceiverOptions: List<FirExpression> = emptyList(),
) : TowerScopeLevel() {
    private val memberScopeLevels = contextReceiverGroup.map {
        MemberScopeTowerLevel(bodyResolveComponents, it, givenExtensionReceiverOptions, false)
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
    private val givenExtensionReceiverOptions: List<FirExpression>,
    private val withHideMembersOnly: Boolean,
    private val includeInnerConstructors: Boolean,
    private val dispatchReceiverForStatics: ExpressionReceiverValue?
) : TowerScopeLevel() {
    private val session: FirSession get() = bodyResolveComponents.session

    fun areThereExtensionReceiverOptions(): Boolean = givenExtensionReceiverOptions.isNotEmpty()

    private fun FirRegularClassSymbol.toResolvedQualifierExpressionReceiver(source: KtSourceElement?): ExpressionReceiverValue {
        val resolvedQualifier = buildResolvedQualifier {
            packageFqName = classId.packageFqName
            relativeClassFqName = classId.relativeClassName
            this.symbol = this@toResolvedQualifierExpressionReceiver
            this.source = source?.fakeElement(KtFakeSourceElementKind.ImplicitReceiver)
        }.apply {
            setTypeOfQualifier(bodyResolveComponents.session)
        }
        return ExpressionReceiverValue(resolvedQualifier)
    }

    // For static entries we may return here FirResolvedQualifier, wrapped in ExpressionReceiverValue
    private fun dispatchReceiverValue(candidate: FirCallableSymbol<*>, callInfo: CallInfo): ReceiverValue? {
        candidate.fir.importedFromObjectOrStaticData?.let { data ->
            val objectClassId = data.objectClassId
            val symbol = session.symbolProvider.getClassLikeSymbolByClassId(objectClassId)
            if (symbol is FirRegularClassSymbol) {
                return symbol.toResolvedQualifierExpressionReceiver(callInfo.callSite.source)
            }
        }

        when {
            candidate is FirBackingFieldSymbol -> {
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
            candidate.isStatic -> {
                return dispatchReceiverForStatics
            }
            else -> return null
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
            val extensionReceiverType = extensionReceiver.resolvedType
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
        callInfo: CallInfo,
        processor: TowerScopeLevelProcessor<T>
    ) {
        candidate.lazyResolveToPhase(FirResolvePhase.TYPES)
        if (withHideMembersOnly && candidate.getAnnotationByClassId(HidesMembers, session) == null) {
            return
        }

        val receiverExpected = withHideMembersOnly || areThereExtensionReceiverOptions()
        val candidateReceiverTypeRef = candidate.fir.receiverParameter?.typeRef
        if (candidateReceiverTypeRef == null == receiverExpected) return

        val dispatchReceiverValue = dispatchReceiverValue(candidate, callInfo)
        if (dispatchReceiverValue == null && shouldSkipCandidateWithInconsistentExtensionReceiver(candidate)) {
            return
        }
        val unwrappedCandidate = candidate.fir.importedFromObjectOrStaticData?.original?.symbol ?: candidate
        @Suppress("UNCHECKED_CAST")
        processor.consumeCandidate(
            unwrappedCandidate as T,
            dispatchReceiverValue?.receiverExpression,
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
            consumeCallableCandidate(candidate, info, processor)
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
            consumeCallableCandidate(candidate, info, processor)
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
                it, dispatchReceiver = null,
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
