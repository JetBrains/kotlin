/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.isStatic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractImportingScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.isExtensionFunctionType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface TowerScopeLevel {

    sealed class Token<out T : AbstractFirBasedSymbol<*>> {
        object Properties : Token<FirPropertySymbol>()

        object Functions : Token<FirFunctionSymbol<*>>()
        object Objects : Token<AbstractFirBasedSymbol<*>>()
    }

    fun <T : AbstractFirBasedSymbol<*>> processElementsByName(
        token: Token<T>,
        name: Name,
        explicitReceiver: AbstractExplicitReceiver<*>?,
        processor: TowerScopeLevelProcessor<T>
    ): ProcessorAction

    interface TowerScopeLevelProcessor<T : AbstractFirBasedSymbol<*>> {
        fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue<*>?,
            builtInExtensionFunctionReceiverValue: ReceiverValue? = null
        )
    }

    abstract class StubTowerScopeLevel : TowerScopeLevel {
        override fun <T : AbstractFirBasedSymbol<*>> processElementsByName(
            token: Token<T>,
            name: Name,
            explicitReceiver: AbstractExplicitReceiver<*>?,
            processor: TowerScopeLevelProcessor<T>
        ): ProcessorAction = ProcessorAction.NEXT
    }

    object Empty : StubTowerScopeLevel()

    class OnlyImplicitReceiver(val implicitReceiverValue: ImplicitReceiverValue<*>) : StubTowerScopeLevel()
}

abstract class SessionBasedTowerLevel(val session: FirSession) : TowerScopeLevel {
    protected fun AbstractFirBasedSymbol<*>.dispatchReceiverValue(): ClassDispatchReceiverValue? {
        return when (this) {
            is FirNamedFunctionSymbol -> fir.dispatchReceiverValue(session)
            is FirPropertySymbol -> fir.dispatchReceiverValue(session)
            is FirFieldSymbol -> fir.dispatchReceiverValue(session)
            is FirClassSymbol -> ClassDispatchReceiverValue(this)
            else -> null
        }
    }

    protected fun FirCallableSymbol<*>.hasConsistentExtensionReceiver(extensionReceiver: ReceiverValue?): Boolean {
        return when {
            extensionReceiver != null -> hasExtensionReceiver()
            else -> fir.receiverTypeRef == null
        }
    }
}

// This is more like "dispatch receiver-based tower level"
// Here we always have an explicit or implicit dispatch receiver, and can access members of its scope
// (which is separated from currently accessible scope, see below)
// So: dispatch receiver = given explicit or implicit receiver (always present)
// So: extension receiver = either none, if dispatch receiver = explicit receiver,
//     or given implicit or explicit receiver, otherwise
class MemberScopeTowerLevel(
    session: FirSession,
    val bodyResolveComponents: BodyResolveComponents,
    val dispatchReceiver: ReceiverValue,
    val implicitExtensionReceiver: ImplicitReceiverValue<*>? = null,
    val implicitExtensionInvokeMode: Boolean = false,
    val scopeSession: ScopeSession
) : SessionBasedTowerLevel(session) {
    private fun <T : AbstractFirBasedSymbol<*>> processMembers(
        output: TowerScopeLevel.TowerScopeLevelProcessor<T>,
        explicitExtensionReceiver: AbstractExplicitReceiver<*>?,
        processScopeMembers: FirScope.(processor: (T) -> Unit) -> Unit
    ) {
        if (implicitExtensionReceiver != null && explicitExtensionReceiver != null) return
        val extensionReceiver = implicitExtensionReceiver ?: explicitExtensionReceiver
        val scope = dispatchReceiver.scope(session, scopeSession) ?: return
        scope.processScopeMembers { candidate ->
            if (candidate is FirCallableSymbol<*> &&
                (implicitExtensionInvokeMode || candidate.hasConsistentExtensionReceiver(extensionReceiver))
            ) {
                val dispatchReceiverValue = NotNullableReceiverValue(dispatchReceiver)
                if (implicitExtensionInvokeMode) {
                    output.consumeCandidate(
                        candidate, dispatchReceiverValue,
                        implicitExtensionReceiverValue = implicitExtensionReceiver
                    )
                    output.consumeCandidate(
                        candidate, dispatchReceiverValue,
                        implicitExtensionReceiverValue = null,
                        builtInExtensionFunctionReceiverValue = implicitExtensionReceiver
                    )
                } else {
                    output.consumeCandidate(candidate, dispatchReceiverValue, implicitExtensionReceiver, null)
                }
            } else if (candidate is FirClassLikeSymbol<*>) {
                output.consumeCandidate(candidate, null, implicitExtensionReceiver, null)
            }
        }

        val withSynthetic = FirSyntheticPropertiesScope(session, scope)
        withSynthetic.processScopeMembers { symbol ->
            output.consumeCandidate(symbol, NotNullableReceiverValue(dispatchReceiver), implicitExtensionReceiver, null)
        }
    }

    override fun <T : AbstractFirBasedSymbol<*>> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        explicitReceiver: AbstractExplicitReceiver<*>?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        val isInvoke = name == OperatorNameConventions.INVOKE && token == TowerScopeLevel.Token.Functions
        if (implicitExtensionInvokeMode && !isInvoke) {
            return ProcessorAction.NEXT
        }
        val explicitExtensionReceiver = if (dispatchReceiver == explicitReceiver) null else explicitReceiver
        val noInnerConstructors = dispatchReceiver is QualifierReceiver
        when (token) {
            TowerScopeLevel.Token.Properties -> processMembers(processor, explicitExtensionReceiver) { symbol ->
                this.processPropertiesByName(name, symbol.cast())
            }
            TowerScopeLevel.Token.Functions -> processMembers(processor, explicitExtensionReceiver) { symbol ->
                this.processFunctionsAndConstructorsByName(name, session, bodyResolveComponents, noInnerConstructors, symbol.cast())
            }
            TowerScopeLevel.Token.Objects -> processMembers(processor, explicitExtensionReceiver) { symbol ->
                this.processClassifiersByName(name, symbol.cast())
            }
        }
        return ProcessorAction.NEXT
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
    val implicitExtensionReceiver: ImplicitReceiverValue<*>? = null,
    private val extensionsOnly: Boolean = false
) : SessionBasedTowerLevel(session) {
    private fun FirCallableSymbol<*>.hasConsistentReceivers(extensionReceiver: ReceiverValue?): Boolean =
        when {
            extensionsOnly && !hasExtensionReceiver() -> false
            !hasConsistentExtensionReceiver(extensionReceiver) -> false
            scope is FirAbstractImportingScope -> true
            else -> true
        }

    override fun <T : AbstractFirBasedSymbol<*>> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        explicitReceiver: AbstractExplicitReceiver<*>?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        if (explicitReceiver != null && implicitExtensionReceiver != null) {
            return ProcessorAction.NEXT
        }
        val extensionReceiver = explicitReceiver ?: implicitExtensionReceiver
        @Suppress("UNCHECKED_CAST")
        when (token) {
            TowerScopeLevel.Token.Properties -> scope.processPropertiesByName(name) { candidate ->
                if (candidate.hasConsistentReceivers(extensionReceiver)) {
                    processor.consumeCandidate(
                        candidate as T, dispatchReceiverValue = null,
                        implicitExtensionReceiverValue = implicitExtensionReceiver
                    )
                }
            }
            TowerScopeLevel.Token.Functions -> scope.processFunctionsAndConstructorsByName(
                name,
                session,
                bodyResolveComponents
            ) { candidate ->
                if (candidate.hasConsistentReceivers(extensionReceiver)) {
                    processor.consumeCandidate(
                        candidate as T, dispatchReceiverValue = null,
                        implicitExtensionReceiverValue = implicitExtensionReceiver
                    )
                }
            }
            TowerScopeLevel.Token.Objects -> scope.processClassifiersByName(name) {
                processor.consumeCandidate(
                    it as T, dispatchReceiverValue = null,
                    implicitExtensionReceiverValue = null
                )
            }
        }
        return ProcessorAction.NEXT
    }
}

class NotNullableReceiverValue(val value: ReceiverValue) : ReceiverValue {
    override val type: ConeKotlinType
        get() = value.type.withNullability(ConeNullability.NOT_NULL)
    override val receiverExpression: FirExpression
        get() = value.receiverExpression
}

fun FirCallableDeclaration<*>.dispatchReceiverValue(session: FirSession): ClassDispatchReceiverValue? {
    // TODO: this is not true atCall least for inner class constructors
    if (this is FirConstructor) return null
    if ((this as? FirMemberDeclaration)?.isStatic == true) return null
    val id = this.symbol.callableId.classId ?: return null
    val symbol = session.firSymbolProvider.getClassLikeSymbolByFqName(id) as? FirClassSymbol ?: return null

    return ClassDispatchReceiverValue(symbol)
}

private fun FirCallableSymbol<*>.hasExtensionReceiver(): Boolean {
    if (fir.receiverTypeRef != null) return true
    return fir.returnTypeRef.isExtensionFunctionType()
}
