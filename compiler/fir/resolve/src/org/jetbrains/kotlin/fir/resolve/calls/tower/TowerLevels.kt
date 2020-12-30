/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.isInner
import org.jetbrains.kotlin.fir.dispatchReceiverClassOrNull
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectData
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

abstract class TowerScopeLevel {

    sealed class Token<out T : AbstractFirBasedSymbol<*>> {
        object Properties : Token<FirVariableSymbol<*>>()
        object Functions : Token<FirFunctionSymbol<*>>()
        object Objects : Token<AbstractFirBasedSymbol<*>>()
    }

    abstract fun processFunctionsByName(name: Name, processor: TowerScopeLevelProcessor<FirFunctionSymbol<*>>): ProcessorAction

    abstract fun processPropertiesByName(name: Name, processor: TowerScopeLevelProcessor<FirVariableSymbol<*>>): ProcessorAction

    abstract fun processObjectsByName(name: Name, processor: TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>): ProcessorAction

    interface TowerScopeLevelProcessor<in T : AbstractFirBasedSymbol<*>> {
        fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ReceiverValue?,
            extensionReceiverValue: ReceiverValue?,
            scope: FirScope,
            builtInExtensionFunctionReceiverValue: ReceiverValue? = null
        )
    }
}

abstract class SessionBasedTowerLevel(val session: FirSession) : TowerScopeLevel() {
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
class MemberScopeTowerLevel(
    session: FirSession,
    private val bodyResolveComponents: BodyResolveComponents,
    val dispatchReceiverValue: ReceiverValue,
    private val extensionReceiver: ReceiverValue? = null,
    private val implicitExtensionInvokeMode: Boolean = false,
    private val scopeSession: ScopeSession
) : SessionBasedTowerLevel(session) {
    private fun <T : AbstractFirBasedSymbol<*>> processMembers(
        output: TowerScopeLevelProcessor<T>,
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit
    ): ProcessorAction {
        var empty = true
        val scope = dispatchReceiverValue.scope(session, scopeSession) ?: return ProcessorAction.NONE
        scope.processScopeMembers { candidate ->
            empty = false
            if (candidate is FirCallableSymbol<*> &&
                (implicitExtensionInvokeMode || candidate.hasConsistentExtensionReceiver(extensionReceiver))
            ) {
                val fir = candidate.fir
                if ((fir as? FirConstructor)?.isInner == false) {
                    return@processScopeMembers
                }

                output.consumeCandidate(
                    candidate, dispatchReceiverValue,
                    extensionReceiverValue = extensionReceiver,
                    scope
                )

                if (implicitExtensionInvokeMode) {
                    output.consumeCandidate(
                        candidate, dispatchReceiverValue,
                        extensionReceiverValue = null,
                        scope,
                        builtInExtensionFunctionReceiverValue = this.extensionReceiver
                    )
                }
            } else if (candidate is FirClassLikeSymbol<*>) {
                output.consumeCandidate(candidate, null, extensionReceiver, scope)
            }
        }

        if (extensionReceiver == null) {
            val withSynthetic = FirSyntheticPropertiesScope(session, scope)
            withSynthetic.processScopeMembers { symbol ->
                empty = false
                output.consumeCandidate(symbol, dispatchReceiverValue, null, scope)
            }
        }
        return if (empty) ProcessorAction.NONE else ProcessorAction.NEXT
    }

    override fun processFunctionsByName(
        name: Name,
        processor: TowerScopeLevelProcessor<FirFunctionSymbol<*>>
    ): ProcessorAction {
        val isInvoke = name == OperatorNameConventions.INVOKE
        if (implicitExtensionInvokeMode && !isInvoke) {
            return ProcessorAction.NEXT
        }
        return processMembers(processor) { consumer ->
            this.processFunctionsAndConstructorsByName(
                name, session, bodyResolveComponents,
                includeInnerConstructors = true,
                processor = {
                    // WARNING, DO NOT CAST FUNCTIONAL TYPE ITSELF
                    @Suppress("UNCHECKED_CAST")
                    consumer(it as FirFunctionSymbol<*>)
                }
            )
        }
    }

    override fun processPropertiesByName(
        name: Name,
        processor: TowerScopeLevelProcessor<FirVariableSymbol<*>>
    ): ProcessorAction {
        return processMembers(processor) { consumer ->
            this.processPropertiesByName(name) {
                // WARNING, DO NOT CAST FUNCTIONAL TYPE ITSELF
                @Suppress("UNCHECKED_CAST")
                consumer(it)
            }
        }
    }

    override fun processObjectsByName(
        name: Name,
        processor: TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ): ProcessorAction {
        return ProcessorAction.NEXT
    }

    override fun replaceReceiverValue(receiverValue: ReceiverValue): SessionBasedTowerLevel {
        return MemberScopeTowerLevel(
            session, bodyResolveComponents, receiverValue, extensionReceiver, implicitExtensionInvokeMode, scopeSession
        )
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
    private fun FirCallableSymbol<*>.hasConsistentReceivers(extensionReceiver: Receiver?): Boolean =
        when {
            extensionsOnly && !hasExtensionReceiver() -> false
            !hasConsistentExtensionReceiver(extensionReceiver) -> false
            else -> true
        }

    private fun dispatchReceiverValue(candidate: FirCallableSymbol<*>): ReceiverValue? {
        candidate.fir.importedFromObjectData?.let { data ->
            val objectClassId = data.objectClassId
            val symbol = session.firSymbolProvider.getClassLikeSymbolByFqName(objectClassId)
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

        val lookupTag = candidate.dispatchReceiverClassOrNull()
        return when {
            candidate !is FirBackingFieldSymbol -> null
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

    private fun <T : AbstractFirBasedSymbol<*>> consumeCallableCandidate(
        candidate: FirCallableSymbol<*>,
        processor: TowerScopeLevelProcessor<T>
    ) {
        if (candidate.hasConsistentReceivers(extensionReceiver)) {
            val dispatchReceiverValue = dispatchReceiverValue(candidate)
            val unwrappedCandidate = candidate.fir.importedFromObjectData?.original?.symbol ?: candidate
            @Suppress("UNCHECKED_CAST")
            processor.consumeCandidate(
                unwrappedCandidate as T, dispatchReceiverValue,
                extensionReceiverValue = extensionReceiver,
                scope
            )
        }
    }

    override fun processFunctionsByName(
        name: Name,
        processor: TowerScopeLevelProcessor<FirFunctionSymbol<*>>
    ): ProcessorAction {
        var empty = true
        scope.processFunctionsAndConstructorsByName(
            name,
            session,
            bodyResolveComponents,
            includeInnerConstructors = includeInnerConstructors
        ) { candidate ->
            empty = false
            consumeCallableCandidate(candidate, processor)
        }
        return if (empty) ProcessorAction.NONE else ProcessorAction.NEXT
    }

    override fun processPropertiesByName(
        name: Name,
        processor: TowerScopeLevelProcessor<FirVariableSymbol<*>>
    ): ProcessorAction {
        var empty = true
        scope.processPropertiesByName(name) { candidate ->
            empty = false
            consumeCallableCandidate(candidate, processor)
        }
        return if (empty) ProcessorAction.NONE else ProcessorAction.NEXT
    }

    override fun processObjectsByName(
        name: Name,
        processor: TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ): ProcessorAction {
        var empty = true
        scope.processClassifiersByName(name) {
            empty = false
            processor.consumeCandidate(
                it, dispatchReceiverValue = null,
                extensionReceiverValue = null,
                scope = scope
            )
        }
        return if (empty) ProcessorAction.NONE else ProcessorAction.NEXT
    }
}

private fun FirCallableSymbol<*>.hasExtensionReceiver(): Boolean {
    return fir.receiverTypeRef != null
}
