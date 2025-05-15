/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.ContextReceiverGroup
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.hasAnnotationWithClassId
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirStaticPhantomThisExpression
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.fir.resolve.calls.stages.isSuperCall
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirActualizingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.HidesMembers
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

enum class ProcessResult {
    FOUND, SCOPE_EMPTY;

    operator fun plus(other: ProcessResult): ProcessResult {
        if (this == FOUND || other == FOUND) return FOUND
        return this
    }
}

abstract class TowerLevel {
    abstract fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult

    abstract fun processPropertiesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult

    abstract fun processObjectsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult
}

/**
 * Here we always have an explicit or implicit dispatch receiver and can access members of its scope
 * (which is separated from the currently accessible scope, see below).
 * So:
 * * dispatch receiver = given explicit or implicit receiver (always present);
 * * extension receiver = either none, if dispatch receiver = explicit receiver,
 *     or given implicit or explicit receiver, otherwise
 */
class DispatchReceiverMemberScopeTowerLevel(
    private val bodyResolveComponents: BodyResolveComponents,
    val dispatchReceiverValue: ReceiverValue,
    private val givenExtensionReceiverOptions: List<FirExpression>,
    private val skipSynthetics: Boolean,
) : TowerLevel() {
    private val scopeSession: ScopeSession get() = bodyResolveComponents.scopeSession
    private val session: FirSession get() = bodyResolveComponents.session

    private fun <T : FirCallableSymbol<*>> processMembers(
        info: CallInfo,
        output: TowerLevelProcessor,
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit
    ): ProcessResult {
        val scope = dispatchReceiverValue.scope(session, scopeSession) ?: return ProcessResult.SCOPE_EMPTY

        val receiverTypeWithoutSmartCast = getOriginalReceiverExpressionIfStableSmartCast()?.resolvedType
        val scopeWithoutSmartcast = receiverTypeWithoutSmartCast?.scope(
            session,
            scopeSession,
            bodyResolveComponents.returnTypeCalculator.callableCopyTypeCalculator,
            requiredMembersPhase = FirResolvePhase.STATUS,
        )

        var processResult: ProcessResult
        if (scopeWithoutSmartcast == null) {
            processResult = scope.processCandidates(processScopeMembers) { candidate ->
                output.consumeCandidate(
                    candidate,
                    dispatchReceiverValue.receiverExpression,
                    givenExtensionReceiverOptions,
                    scope,
                    isFromOriginalTypeInPresenceOfSmartCast = false
                )
            }
        } else {
            val map: MutableMap<T, MemberFromSmartcastScope<T>> = mutableMapOf()

            processResult = scopeWithoutSmartcast.processCandidates(processScopeMembers) { candidate ->
                map[candidate] =
                    MemberFromSmartcastScope(MemberWithBaseScope(candidate, scopeWithoutSmartcast), DispatchReceiverToUse.UnwrapSmartcast)
            }

            processResult += scope.processCandidates(processScopeMembers) { memberFromSmartcast ->
                val keyMember = unwrapSubstitutionOverrideForSmartcastedThisAccessInAnonymousInitializer(memberFromSmartcast)
                    ?: memberFromSmartcast
                val existing = map[keyMember]

                // If both scopes return the same symbol, we want to prefer the candidate from the original scope without smartcast
                // with two exceptions:
                // - When the smart-casted type is always null, we want to return it and report UNSAFE_CALL.
                // - When the original type can be null, in this case the smart-case either makes it not-null or the call is red anyway.
                if (existing == null || dispatchReceiverValue.type?.isNullableNothing == true || receiverTypeWithoutSmartCast.canBeNull(session)) {
                    map[memberFromSmartcast] = MemberFromSmartcastScope(
                        MemberWithBaseScope(memberFromSmartcast, scope),
                        DispatchReceiverToUse.SmartcastWithoutUnwrapping
                    )
                } else {
                    existing.dispatchReceiverToUse = DispatchReceiverToUse.SmartcastIfUnwrappedInvisible
                }
            }

            // all the candidates, both from original type and smart cast
            consumeCandidates(output, candidatesWithSmartcast = map)
        }

        val dispatchReceiverType = dispatchReceiverValue.type
        if (givenExtensionReceiverOptions.isEmpty() && !skipSynthetics && dispatchReceiverType != null) {
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
                    CallableCopyTypeCalculator.DoNothing,
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
                isSuperCall = info.callSite.isSuperCall(),
            )

            withSynthetic?.processScopeMembers { symbol ->
                processResult = ProcessResult.FOUND
                output.consumeCandidate(
                    symbol,
                    dispatchReceiverValue.receiverExpression,
                    givenExtensionReceiverOptions = emptyList(),
                    scope
                )
            }
        }
        return processResult
    }

    /**
     * Inside `init` blocks we want to prefer the candidate from the original scope if the candidate from the smartcast is regular
     *   substitution override to support cases like this:
     *
     * ```
     * open class Base<T> {
     *     val x: Int
     *
     *     init {
     *         if (this is Derived) {
     *             x = 1 // <--------
     *         } else {
     *             x = 2
     *         }
     *     }
     * }
     *
     * class Derived : Base<String>()
     * ```
     *
     * It's important to resolve to `Base.x` in the highlighted place instead of `Derived.x`, so fir2ir will generate proper
     *   SetField call instead of setter call, which breaks the initialization of the class
     */
    private fun <T : FirCallableSymbol<*>> unwrapSubstitutionOverrideForSmartcastedThisAccessInAnonymousInitializer(
        candidateFromSmartCast: T
    ): T? {
        if (!candidateFromSmartCast.isSubstitutionOverride) return null
        val smartcastedReceiver = dispatchReceiverValue.receiverExpression as? FirSmartCastExpression ?: return null
        val thisReceiver = smartcastedReceiver.originalExpression as? FirThisReceiverExpression ?: return null
        val classSymbol = thisReceiver.calleeReference.boundSymbol as? FirClassSymbol<*> ?: return null
        return runIf(classSymbol in bodyResolveComponents.towerDataContext.classesUnderInitialization) {
            @Suppress("UNCHECKED_CAST")
            candidateFromSmartCast.unwrapSubstitutionOverrides<FirCallableSymbol<*>>() as T
        }
    }

    private enum class DispatchReceiverToUse(val unwrapSmartcast: Boolean) {
        UnwrapSmartcast(true),
        SmartcastWithoutUnwrapping(false),
        SmartcastIfUnwrappedInvisible(true),
    }

    private class MemberFromSmartcastScope<T : FirCallableSymbol<*>>(
        val memberWithBaseScope: MemberWithBaseScope<T>,
        var dispatchReceiverToUse: DispatchReceiverToUse,
    )

    private inline fun <T : FirCallableSymbol<*>> FirTypeScope.processCandidates(
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit,
        crossinline candidateProcessor: (T) -> Unit,
    ): ProcessResult {
        var result = ProcessResult.SCOPE_EMPTY
        processScopeMembers { candidate ->
            result = ProcessResult.FOUND
            if (candidate.hasConsistentExtensionReceiver(givenExtensionReceiverOptions)) {
                candidateProcessor(candidate)
            }
        }
        return result
    }

    /**
     * The method consumes candidates if only there's a smart cast type on a dispatch receiver,
     * and candidates are present both in smart cast and original types.
     * `isFromSmartCast[candidate] == true` if exactly that member is present in smart cast type.
     */
    private fun <T : FirCallableSymbol<*>> consumeCandidates(
        output: TowerLevelProcessor,
        candidatesWithSmartcast: Map<T, MemberFromSmartcastScope<T>>,
    ) {
        for (scopeWithSmartcast in candidatesWithSmartcast.values) {
            val (candidate, scope) = scopeWithSmartcast.memberWithBaseScope

            if (candidate.hasConsistentExtensionReceiver(givenExtensionReceiverOptions)) {
                val dispatchReceiverToUse = scopeWithSmartcast.dispatchReceiverToUse
                val isFromOriginalTypeInPresenceOfSmartCast = dispatchReceiverToUse.unwrapSmartcast
                val dispatchReceiver = when {
                    isFromOriginalTypeInPresenceOfSmartCast -> getOriginalReceiverExpressionIfStableSmartCast()
                    else -> dispatchReceiverValue.receiverExpression
                }

                val applicability = output.consumeCandidate(
                    candidate,
                    dispatchReceiver,
                    givenExtensionReceiverOptions,
                    scope,
                    isFromOriginalTypeInPresenceOfSmartCast = isFromOriginalTypeInPresenceOfSmartCast
                )

                if (applicability == CandidateApplicability.K2_VISIBILITY_ERROR && dispatchReceiverToUse == DispatchReceiverToUse.SmartcastIfUnwrappedInvisible) {
                    output.consumeCandidate(
                        candidate,
                        dispatchReceiverValue.receiverExpression,
                        givenExtensionReceiverOptions,
                        scope,
                        isFromOriginalTypeInPresenceOfSmartCast = false
                    )
                }
            }
        }
    }

    private fun getOriginalReceiverExpressionIfStableSmartCast() =
        (dispatchReceiverValue.receiverExpression as? FirSmartCastExpression)
            ?.takeIf { it.isStable }
            ?.originalExpression

    override fun processFunctionsByName(
        info: CallInfo,
        processor: TowerLevelProcessor
    ): ProcessResult {
        val lookupTracker = session.lookupTracker
        return processMembers(info, processor) { consumer ->
            dispatchReceiverValue.type?.let { lookupTracker?.recordCallLookup(info, it) }
            this.processFunctionsAndConstructorsByName(
                info, session, bodyResolveComponents,
                ConstructorFilter.OnlyInner,
                processor = {
                    lookupTracker?.recordCallableCandidateAsLookup(it, info.callSite.source, info.containingFile.source)
                    // WARNING, DO NOT CAST FUNCTIONAL TYPE ITSELF
                    consumer(it as FirFunctionSymbol<*>)
                }
            )
        }
    }

    override fun processPropertiesByName(
        info: CallInfo,
        processor: TowerLevelProcessor
    ): ProcessResult {
        val lookupTracker = session.lookupTracker
        return processMembers(info, processor) { consumer ->
            dispatchReceiverValue.type?.let { lookupTracker?.recordCallLookup(info, it) }
            this.processPropertiesByName(info.name) {
                lookupTracker?.recordCallableCandidateAsLookup(it, info.callSite.source, info.containingFile.source)
                consumer(it)
            }
        }
    }

    override fun processObjectsByName(
        info: CallInfo,
        processor: TowerLevelProcessor
    ): ProcessResult {
        return ProcessResult.FOUND
    }

    private fun FirCallableSymbol<*>.hasConsistentExtensionReceiver(givenExtensionReceivers: List<FirExpression>): Boolean {
        return givenExtensionReceivers.isNotEmpty() == hasExtensionReceiver()
    }
}

class ContextReceiverGroupMemberScopeTowerLevel(
    bodyResolveComponents: BodyResolveComponents,
    contextReceiverGroup: ContextReceiverGroup,
    givenExtensionReceiverOptions: List<FirExpression> = emptyList(),
) : TowerLevel() {
    private val dispatchReceiverMemberScopeTowerLevels = contextReceiverGroup.map {
        DispatchReceiverMemberScopeTowerLevel(bodyResolveComponents, it, givenExtensionReceiverOptions, false)
    }

    override fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        return dispatchReceiverMemberScopeTowerLevels.minOf { it.processFunctionsByName(info, processor) }
    }

    override fun processPropertiesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        return dispatchReceiverMemberScopeTowerLevels.minOf { it.processPropertiesByName(info, processor) }
    }

    override fun processObjectsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        return dispatchReceiverMemberScopeTowerLevels.minOf { it.processObjectsByName(info, processor) }
    }
}

/**
 * We can access here members of currently accessible scope which is not influenced by explicit receiver.
 * We can either have no explicit receiver at all, or it can be an extension receiver.
 * An explicit receiver never can be a dispatch receiver at this level.
 * * dispatch receiver = strictly none (EXCEPTIONS: importing scopes with import from objects, synthetic field variable)
 * * extension receiver = either none or explicit (if explicit receiver exists, it always *should* be an extension receiver)
 */
internal class ScopeBasedTowerLevel(
    private val bodyResolveComponents: BodyResolveComponents,
    givenScope: FirScope,
    private val givenExtensionReceiverOptions: List<FirExpression>,
    private val withHideMembersOnly: Boolean,
    private val constructorFilter: ConstructorFilter,
    private val dispatchReceiverForStatics: ExpressionReceiverValue?
) : TowerLevel() {
    private val session: FirSession get() = bodyResolveComponents.session

    private val scope = if (session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
        FirActualizingScope(givenScope)
    } else {
        givenScope
    }

    fun areThereExtensionReceiverOptions(): Boolean = givenExtensionReceiverOptions.isNotEmpty()

    private fun FirRegularClassSymbol.toResolvedQualifierExpressionReceiver(source: KtSourceElement?): ExpressionReceiverValue {
        val resolvedQualifier = buildResolvedQualifier {
            packageFqName = classId.packageFqName
            relativeClassFqName = classId.relativeClassName
            this.symbol = this@toResolvedQualifierExpressionReceiver
            this.source = source?.fakeElement(KtFakeSourceElementKind.ImplicitReceiver)
        }.apply {
            setTypeOfQualifier(bodyResolveComponents)
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
                        bodyResolveComponents.implicitValueStorage.lastDispatchReceiver { implicitReceiverValue ->
                            implicitReceiverValue.type.fullyExpandedType(session).lookupTagIfAny == lookupTag
                        }
                    }
                    else -> null
                }
            }
            candidate.isStatic -> {
                return dispatchReceiverForStatics
            }
            else -> return null
        }
    }

    private fun shouldSkipCandidateWithInconsistentExtensionReceiver(candidate: FirCallableSymbol<*>): Boolean {
        return shouldSkipCandidateWithInconsistentValueExtensionReceiver(candidate) || shouldSkipCandidateWithInconsistentStaticExtensionReceiver(candidate)
    }

    private fun shouldSkipCandidateWithInconsistentValueExtensionReceiver(candidate: FirCallableSymbol<*>): Boolean {
        if (candidate.resolvedReceiverType != null && givenExtensionReceiverOptions.all { it is FirStaticPhantomThisExpression }) return true

        // Pre-check explicit extension receiver for default package top-level members
        if (scope !is FirDefaultStarImportingScope || !areThereExtensionReceiverOptions()) return false

        val declarationReceiverType = candidate.resolvedReceiverType as? ConeClassLikeType ?: return false
        val startProjectedDeclarationReceiverType = declarationReceiverType.lookupTag.constructClassType(
            declarationReceiverType.typeArguments.map { ConeStarProjection }.toTypedArray(),
            isMarkedNullable = true
        )

        return givenExtensionReceiverOptions.none { extensionReceiver ->
            if (extensionReceiver is FirStaticPhantomThisExpression) return@none false

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

    private fun shouldSkipCandidateWithInconsistentStaticExtensionReceiver(candidate: FirCallableSymbol<*>): Boolean {
        if (candidate.fir.staticReceiverParameter != null && givenExtensionReceiverOptions.all { it !is FirStaticPhantomThisExpression }) return true
        val staticExtensionReceiverType = candidate.fir.staticReceiverParameter?.coneTypeOrNull?.toClassSymbol(session) ?: return false
        return givenExtensionReceiverOptions.none { extensionReceiver ->
            if (extensionReceiver !is FirStaticPhantomThisExpression) return@none false
            extensionReceiver.classSymbol == staticExtensionReceiverType
        }
    }

    private fun consumeCallableCandidate(
        candidate: FirCallableSymbol<*>,
        callInfo: CallInfo,
        processor: TowerLevelProcessor
    ) {
        candidate.lazyResolveToPhase(FirResolvePhase.TYPES)
        if (withHideMembersOnly && !candidate.hasAnnotationWithClassId(HidesMembers, session)) {
            return
        }

        val receiverExpected = withHideMembersOnly || areThereExtensionReceiverOptions()
        val candidateReceiverTypeRef = candidate.fir.receiverParameter?.typeRef
        val candidateStaticReceiverType = candidate.fir.staticReceiverParameter
        val hasActualReceiver = candidateReceiverTypeRef != null || candidateStaticReceiverType != null
        if (hasActualReceiver != receiverExpected) return

        val dispatchReceiverValue = dispatchReceiverValue(candidate, callInfo)
        if (dispatchReceiverValue == null && shouldSkipCandidateWithInconsistentExtensionReceiver(candidate)) {
            return
        }
        val unwrappedCandidate = candidate.fir.importedFromObjectOrStaticData?.original?.symbol ?: candidate
        processor.consumeCandidate(
            unwrappedCandidate,
            dispatchReceiverValue?.receiverExpression,
            givenExtensionReceiverOptions,
            scope
        )
    }

    override fun processFunctionsByName(
        info: CallInfo,
        processor: TowerLevelProcessor
    ): ProcessResult {
        val lookupTracker = session.lookupTracker
        var empty = true
        lookupTracker?.recordCallLookup(info, scope.scopeOwnerLookupNames)
        scope.processFunctionsAndConstructorsByName(
            info,
            session,
            bodyResolveComponents,
            constructorFilter
        ) { candidate ->
            lookupTracker?.recordCallableCandidateAsLookup(candidate, info.callSite.source, info.containingFile.source)
            empty = false
            consumeCallableCandidate(candidate, info, processor)
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }

    override fun processPropertiesByName(
        info: CallInfo,
        processor: TowerLevelProcessor
    ): ProcessResult {
        val lookupTracker = session.lookupTracker
        var empty = true
        lookupTracker?.recordCallLookup(info, scope.scopeOwnerLookupNames)
        scope.processPropertiesByName(info.name) { candidate ->
            lookupTracker?.recordCallableCandidateAsLookup(candidate, info.callSite.source, info.containingFile.source)
            empty = false
            consumeCallableCandidate(candidate, info, processor)
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }

    override fun processObjectsByName(
        info: CallInfo,
        processor: TowerLevelProcessor
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
    return fir.receiverParameter != null || fir.staticReceiverParameter != null
}
