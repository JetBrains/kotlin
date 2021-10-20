/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectData
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.SmartList

enum class ProcessResult {
    FOUND, SCOPE_EMPTY;

    operator fun plus(other: ProcessResult): ProcessResult {
        if (this == FOUND || other == FOUND) return FOUND
        return this
    }
}

abstract class TowerLevel {

    sealed class Token<out T : FirBasedSymbol<*>> {
        object Properties : Token<FirVariableSymbol<*>>()
        object Functions : Token<FirFunctionSymbol<*>>()
        object Objects : Token<FirBasedSymbol<*>>()
    }

    abstract fun processFunctionsByName(info: CallInfo, processor: Processor<FirFunctionSymbol<*>>): ProcessResult

    abstract fun processPropertiesByName(info: CallInfo, processor: Processor<FirVariableSymbol<*>>): ProcessResult

    abstract fun processObjectsByName(info: CallInfo, processor: Processor<FirBasedSymbol<*>>): ProcessResult

    interface Processor<in T : FirBasedSymbol<*>> {
        fun consumeSymbol(
            symbol: T,
            dispatchReceiverValue: ReceiverValue?,
            extensionReceiverValue: ReceiverValue?,
            scope: FirScope,
            builtInExtensionFunctionReceiverValue: ReceiverValue? = null,
            objectsByName: Boolean = false,
        )
    }
}

abstract class SessionBasedTowerLevel(val session: FirSession) : TowerLevel() {
    protected fun FirCallableSymbol<*>.hasConsistentExtensionReceiver(extensionReceiver: Receiver?): Boolean {
        return (extensionReceiver != null) == hasExtensionReceiver()
    }

    open fun replaceReceiverValue(receiverValue: ReceiverValue) = this
}

// This is more like "dispatch receiver-based tower level"
// Here we always have an explicit or implicit dispatch receiver, and can access members of its scope
// (which is separated from currently accessible scope, see below)
// So: dispatch receiver = given explicit or implicit receiver (always present)
// So: extension receiver = either none, if dispatch receiver = explicit receiver,
//     or given implicit or explicit receiver, otherwise
class MemberTowerLevel(
    session: FirSession,
    private val bodyResolveComponents: BodyResolveComponents,
    val dispatchReceiverValue: ReceiverValue,
    private val extensionReceiver: ReceiverValue? = null,
    private val implicitExtensionInvokeMode: Boolean = false,
    private val scopeSession: ScopeSession
) : SessionBasedTowerLevel(session) {
    private fun <T : FirBasedSymbol<*>> processMembers(
        processor: Processor<T>,
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit
    ): ProcessResult {
        var empty = true
        val scope = dispatchReceiverValue.scope(session, scopeSession) ?: return ProcessResult.SCOPE_EMPTY
        scope.processScopeMembers { symbol ->
            empty = false
            if (symbol is FirCallableSymbol<*> &&
                (implicitExtensionInvokeMode || symbol.hasConsistentExtensionReceiver(extensionReceiver))
            ) {
                val fir = symbol.fir
                if ((fir as? FirConstructor)?.isInner == false) {
                    return@processScopeMembers
                }

                processor.consumeSymbol(
                    symbol,
                    dispatchReceiverValue,
                    extensionReceiver,
                    scope
                )

                if (implicitExtensionInvokeMode) {
                    processor.consumeSymbol(
                        symbol,
                        dispatchReceiverValue,
                        null,
                        scope,
                        builtInExtensionFunctionReceiverValue = this.extensionReceiver
                    )
                }
            } else if (symbol is FirClassLikeSymbol<*>) {
                processor.consumeSymbol(
                    symbol,
                    null,
                    extensionReceiver,
                    scope
                )
            }
        }

        if (extensionReceiver == null) {
            val withSynthetic = FirSyntheticPropertiesScope(session, scope)
            withSynthetic.processScopeMembers { symbol ->
                empty = false
                processor.consumeSymbol(
                    symbol,
                    dispatchReceiverValue,
                    null,
                    scope
                )
            }
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }

    override fun processFunctionsByName(
        info: CallInfo,
        processor: Processor<FirFunctionSymbol<*>>
    ): ProcessResult {
        val isInvoke = info.name == OperatorNameConventions.INVOKE
        if (implicitExtensionInvokeMode && !isInvoke) {
            return ProcessResult.FOUND
        }
        val lookupTracker = session.lookupTracker
        return processMembers(processor) { consumer ->
            withMemberCallLookup(lookupTracker, info) { lookupCtx ->
                this.processFunctionsAndConstructorsByName(
                    info.name, session, bodyResolveComponents,
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
        processor: Processor<FirVariableSymbol<*>>
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
        processor: Processor<FirBasedSymbol<*>>
    ): ProcessResult {
        return ProcessResult.FOUND
    }

    override fun replaceReceiverValue(receiverValue: ReceiverValue): SessionBasedTowerLevel {
        return MemberTowerLevel(
            session, bodyResolveComponents, receiverValue, extensionReceiver, implicitExtensionInvokeMode, scopeSession
        )
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
}

// This is more like "scope-based tower level"
// We can access here members of currently accessible scope which is not influenced by explicit receiver
// We can either have no explicit receiver at all, or it can be an extension receiver
// An explicit receiver never can be a dispatch receiver at this level
// So: dispatch receiver = strictly none (EXCEPTIONS: importing scopes with import from objects, synthetic field variable)
// So: extension receiver = either none or explicit
// (if explicit receiver exists, it always *should* be an extension receiver)
class ScopeTowerLevel(
    session: FirSession,
    private val bodyResolveComponents: BodyResolveComponents,
    val scope: FirScope,
    val extensionReceiver: ReceiverValue?,
    private val extensionsOnly: Boolean,
    private val includeInnerConstructors: Boolean
) : SessionBasedTowerLevel(session) {

    private fun dispatchReceiverValue(symbol: FirCallableSymbol<*>): ReceiverValue? {
        symbol.fir.importedFromObjectData?.let { data ->
            val objectClassId = data.objectClassId
            val objectSymbol = session.symbolProvider.getClassLikeSymbolByClassId(objectClassId)
            if (objectSymbol is FirRegularClassSymbol) {
                val resolvedQualifier = buildResolvedQualifier {
                    packageFqName = objectClassId.packageFqName
                    relativeClassFqName = objectClassId.relativeClassName
                    this.symbol = objectSymbol
                }.apply {
                    resultType = bodyResolveComponents.typeForQualifier(this)
                }
                return ExpressionReceiverValue(resolvedQualifier)
            }
        }

        if (symbol !is FirBackingFieldSymbol) {
            return null
        }

        val lookupTag = symbol.fir.propertySymbol.dispatchReceiverClassOrNull()
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

    private fun shouldSkipSymbolWithInconsistentExtensionReceiver(symbol: FirCallableSymbol<*>): Boolean {
        // Pre-check explicit extension receiver for default package top-level members
        if (scope is FirDefaultStarImportingScope && extensionReceiver != null) {
            val extensionReceiverType = extensionReceiver.type
            if (extensionReceiverType is ConeClassLikeType) {
                val declarationReceiverType = symbol.fir.receiverTypeRef?.coneType
                if (declarationReceiverType is ConeClassLikeType) {
                    if (!AbstractTypeChecker.isSubtypeOf(
                            session.typeContext,
                            extensionReceiverType,
                            declarationReceiverType.lookupTag.constructClassType(
                                declarationReceiverType.typeArguments.map { ConeStarProjection }.toTypedArray(),
                                isNullable = true
                            )
                        )
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun <T : FirBasedSymbol<*>> consumeCallableSymbol(
        symbol: FirCallableSymbol<*>,
        processor: Processor<T>
    ) {
        val symbolReceiverTypeRef = symbol.fir.receiverTypeRef
        val receiverExpected = extensionsOnly || extensionReceiver != null
        if (symbolReceiverTypeRef == null == receiverExpected) return
        val dispatchReceiverValue = dispatchReceiverValue(symbol)
        if (dispatchReceiverValue == null && shouldSkipSymbolWithInconsistentExtensionReceiver(symbol)) {
            return
        }
        val unwrappedSymbol = symbol.fir.importedFromObjectData?.original?.symbol ?: symbol
        @Suppress("UNCHECKED_CAST")
        processor.consumeSymbol(
            unwrappedSymbol as T,
            dispatchReceiverValue,
            extensionReceiver,
            scope
        )
    }

    override fun processFunctionsByName(
        info: CallInfo,
        processor: Processor<FirFunctionSymbol<*>>
    ): ProcessResult {
        var empty = true
        session.lookupTracker?.recordCallLookup(info, scope.scopeOwnerLookupNames)
        scope.processFunctionsAndConstructorsByName(
            info.name,
            session,
            bodyResolveComponents,
            includeInnerConstructors = includeInnerConstructors
        ) { symbol ->
            empty = false
            consumeCallableSymbol(symbol, processor)
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }

    override fun processPropertiesByName(
        info: CallInfo,
        processor: Processor<FirVariableSymbol<*>>
    ): ProcessResult {
        var empty = true
        session.lookupTracker?.recordCallLookup(info, scope.scopeOwnerLookupNames)
        scope.processPropertiesByName(info.name) { symbol ->
            empty = false
            consumeCallableSymbol(symbol, processor)
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }

    override fun processObjectsByName(
        info: CallInfo,
        processor: Processor<FirBasedSymbol<*>>
    ): ProcessResult {
        var empty = true
        session.lookupTracker?.recordCallLookup(info, scope.scopeOwnerLookupNames)
        scope.processClassifiersByName(info.name) {
            empty = false
            processor.consumeSymbol(
                it,
                null,
                null,
                scope,
                objectsByName = true
            )
        }
        return if (empty) ProcessResult.SCOPE_EMPTY else ProcessResult.FOUND
    }
}

private fun FirCallableSymbol<*>.hasExtensionReceiver(): Boolean {
    return fir.receiverTypeRef != null
}
