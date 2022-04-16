/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.ContextReceiverGroup
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.typeForQualifier
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectData
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.HidesMembers
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.SmartList

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

    private fun <T : FirBasedSymbol<*>> processMembers(
        output: TowerScopeLevelProcessor<T>,
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit
    ): ProcessResult {
        val scope = dispatchReceiverValue.scope(session, scopeSession) ?: return ProcessResult.SCOPE_EMPTY
        var (empty, candidates) = scope.collectCandidates(processScopeMembers)
        consumeCandidates(output, candidates.map { scope to it })

        if (givenExtensionReceiverOptions.isEmpty()) {
            val withSynthetic = FirSyntheticPropertiesScope(session, scope)
            withSynthetic.processScopeMembers { symbol ->
                empty = false
                output.consumeCandidate(symbol, dispatchReceiverValue, givenExtensionReceiverOptions = emptyList(), scope)
            }
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }

    private fun <T : FirBasedSymbol<*>> FirTypeScope.collectCandidates(
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit
    ): Pair<Boolean, List<T>> {
        var empty = true
        val result = mutableListOf<T>()
        processScopeMembers { candidate ->
            empty = false
            if (candidate is FirCallableSymbol<*> && candidate.hasConsistentExtensionReceiver(givenExtensionReceiverOptions)) {
                val fir = candidate.fir
                if ((fir as? FirConstructor)?.isInner == false) {
                    return@processScopeMembers
                }
                result += candidate
            } else if (candidate is FirClassLikeSymbol<*>) {
                result += candidate
            }
        }
        return empty to result
    }

    private fun <T : FirBasedSymbol<*>> consumeCandidates(
        output: TowerScopeLevelProcessor<T>,
        candidatesWithScope: List<Pair<FirScope, T>>
    ) {
        for ((scope, candidate) in candidatesWithScope) {
            if (candidate is FirCallableSymbol<*> && candidate.hasConsistentExtensionReceiver(givenExtensionReceiverOptions)) {
                output.consumeCandidate(
                    candidate, dispatchReceiverValue,
                    givenExtensionReceiverOptions,
                    scope
                )
            } else if (candidate is FirClassLikeSymbol<*>) {
                output.consumeCandidate(candidate, null, givenExtensionReceiverOptions, scope)
            }
        }
    }

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
                        @Suppress("UNCHECKED_CAST")
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
                    // WARNING, DO NOT CAST FUNCTIONAL TYPE ITSELF
                    @Suppress("UNCHECKED_CAST")
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

    private fun dispatchReceiverValue(candidate: FirCallableSymbol<*>): ReceiverValue? {
        candidate.fir.importedFromObjectData?.let { data ->
            val objectClassId = data.objectClassId
            val symbol = session.symbolProvider.getClassLikeSymbolByClassId(objectClassId)
            if (symbol is FirRegularClassSymbol) {
                val resolvedQualifier = buildResolvedQualifier {
                    packageFqName = objectClassId.packageFqName
                    relativeClassFqName = objectClassId.relativeClassName
                    this.symbol = symbol
                }.apply {
                    resultType = bodyResolveComponents.typeForQualifier(this)
                }
                return ExpressionReceiverValue(resolvedQualifier)
            }
        }

        if (candidate !is FirBackingFieldSymbol) {
            return null
        }

        val lookupTag = candidate.fir.propertySymbol.dispatchReceiverClassOrNull()
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
        val candidateReceiverTypeRef = candidate.fir.receiverTypeRef
        if (withHideMembersOnly && candidate.getAnnotationByClassId(HidesMembers) == null) {
            return
        }
        val receiverExpected = withHideMembersOnly || areThereExtensionReceiverOptions()
        if (candidateReceiverTypeRef == null == receiverExpected) return
        val dispatchReceiverValue = dispatchReceiverValue(candidate)
        if (dispatchReceiverValue == null && shouldSkipCandidateWithInconsistentExtensionReceiver(candidate)) {
            return
        }
        val unwrappedCandidate = candidate.fir.importedFromObjectData?.original?.symbol ?: candidate
        @Suppress("UNCHECKED_CAST")
        processor.consumeCandidate(
            unwrappedCandidate as T, dispatchReceiverValue,
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
                it, dispatchReceiverValue = null,
                givenExtensionReceiverOptions = emptyList(),
                scope = scope,
                objectsByName = true
            )
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }
}

private fun FirCallableSymbol<*>.hasExtensionReceiver(): Boolean {
    return fir.receiverTypeRef != null
}
