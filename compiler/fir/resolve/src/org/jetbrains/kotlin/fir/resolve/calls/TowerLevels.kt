/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirImportImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeAbbreviatedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

interface TowerScopeLevel {

    sealed class Token<out T : AbstractFirBasedSymbol<*>> {
        object Properties : Token<FirPropertySymbol>()

        object Functions : Token<FirFunctionSymbol<*>>()
        object Objects : Token<AbstractFirBasedSymbol<*>>()
    }

    fun <T : AbstractFirBasedSymbol<*>> processElementsByName(
        token: Token<T>,
        name: Name,
        explicitReceiver: ExpressionReceiverValue?,
        processor: TowerScopeLevelProcessor<T>
    ): ProcessorAction

    interface TowerScopeLevelProcessor<T : AbstractFirBasedSymbol<*>> {
        fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ClassDispatchReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue<*>?
        ): ProcessorAction
    }

    object Empty : TowerScopeLevel {
        override fun <T : AbstractFirBasedSymbol<*>> processElementsByName(
            token: Token<T>,
            name: Name,
            explicitReceiver: ExpressionReceiverValue?,
            processor: TowerScopeLevelProcessor<T>
        ): ProcessorAction = ProcessorAction.NEXT
    }
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
        val hasExtensionReceiver = hasExtensionReceiver()
        return hasExtensionReceiver == (extensionReceiver != null)
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
    val scopeSession: ScopeSession
) : SessionBasedTowerLevel(session) {
    private fun <T : AbstractFirBasedSymbol<*>> processMembers(
        output: TowerScopeLevel.TowerScopeLevelProcessor<T>,
        explicitExtensionReceiver: ExpressionReceiverValue?,
        processScopeMembers: FirScope.(processor: (T) -> ProcessorAction) -> ProcessorAction
    ): ProcessorAction {
        if (implicitExtensionReceiver != null && explicitExtensionReceiver != null) return ProcessorAction.NEXT
        val extensionReceiver = implicitExtensionReceiver ?: explicitExtensionReceiver
        val scope = dispatchReceiver.scope(session, scopeSession) ?: return ProcessorAction.NEXT
        if (scope.processScopeMembers { candidate ->
                if (candidate is FirCallableSymbol<*> && candidate.hasConsistentExtensionReceiver(extensionReceiver)) {
                    // NB: we do not check dispatchReceiverValue != null here,
                    // because of objects & constructors (see comments in dispatchReceiverValue() implementation)
                    output.consumeCandidate(candidate, candidate.dispatchReceiverValue(), implicitExtensionReceiver)
                } else if (candidate is FirClassLikeSymbol<*>) {
                    output.consumeCandidate(candidate, null, implicitExtensionReceiver)
                } else {
                    ProcessorAction.NEXT
                }
            }.stop()
        ) return ProcessorAction.STOP
        val withSynthetic = FirSyntheticPropertiesScope(session, scope, ReturnTypeCalculatorWithJump(session, scopeSession))
        return withSynthetic.processScopeMembers { symbol ->
            output.consumeCandidate(symbol, symbol.dispatchReceiverValue(), implicitExtensionReceiver)
        }
    }

    override fun <T : AbstractFirBasedSymbol<*>> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        explicitReceiver: ExpressionReceiverValue?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        val explicitExtensionReceiver = if (dispatchReceiver == explicitReceiver) null else explicitReceiver
        return when (token) {
            TowerScopeLevel.Token.Properties -> processMembers(processor, explicitExtensionReceiver) { symbol ->
                this.processPropertiesByName(name, symbol.cast())
            }
            TowerScopeLevel.Token.Functions -> processMembers(processor, explicitExtensionReceiver) { symbol ->
                this.processFunctionsAndConstructorsByName(name, session, bodyResolveComponents, symbol.cast())
            }
            TowerScopeLevel.Token.Objects -> processMembers(processor, explicitExtensionReceiver) { symbol ->
                this.processClassifiersByName(name, symbol.cast())
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
    val bodyResolveComponents: BodyResolveComponents,
    val scope: FirScope,
    val implicitExtensionReceiver: ImplicitReceiverValue<*>? = null
) : SessionBasedTowerLevel(session) {
    private fun FirCallableSymbol<*>.hasConsistentReceivers(extensionReceiver: ReceiverValue?): Boolean =
        when {
            !hasConsistentExtensionReceiver(extensionReceiver) -> false
            scope is FirAbstractImportingScope -> true
            else -> dispatchReceiverValue().let { it == null || it.klassSymbol.fir.classKind == ClassKind.OBJECT }
        }

    override fun <T : AbstractFirBasedSymbol<*>> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        explicitReceiver: ExpressionReceiverValue?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        if (explicitReceiver != null && implicitExtensionReceiver != null) {
            return ProcessorAction.NEXT
        }
        val extensionReceiver = explicitReceiver ?: implicitExtensionReceiver
        @Suppress("UNCHECKED_CAST")
        return when (token) {
            TowerScopeLevel.Token.Properties -> scope.processPropertiesByName(name) { candidate ->
                if (candidate.hasConsistentReceivers(extensionReceiver)) {
                    val dispatchReceiverValue = when (candidate) {
                        is FirBackingFieldSymbol -> candidate.fir.symbol.dispatchReceiverValue()
                        else -> candidate.dispatchReceiverValue()
                    }
                    processor.consumeCandidate(
                        candidate as T, dispatchReceiverValue = dispatchReceiverValue,
                        implicitExtensionReceiverValue = implicitExtensionReceiver
                    )
                } else {
                    ProcessorAction.NEXT
                }
            }
            TowerScopeLevel.Token.Functions -> scope.processFunctionsAndConstructorsByName(
                name,
                session,
                bodyResolveComponents
            ) { candidate ->
                if (candidate.hasConsistentReceivers(extensionReceiver)) {
                    processor.consumeCandidate(
                        candidate as T, dispatchReceiverValue = candidate.dispatchReceiverValue(),
                        implicitExtensionReceiverValue = implicitExtensionReceiver
                    )
                } else {
                    ProcessorAction.NEXT
                }
            }
            TowerScopeLevel.Token.Objects -> scope.processClassifiersByName(name) {
                processor.consumeCandidate(
                    it as T, dispatchReceiverValue = null,
                    implicitExtensionReceiverValue = null
                )
            }
        }
    }
}

/**
 *  Handles only statics and top-levels, DOES NOT handle objects/companions members
 */
class QualifiedReceiverTowerLevel(
    session: FirSession,
    private val bodyResolveComponents: BodyResolveComponents
) : SessionBasedTowerLevel(session) {
    override fun <T : AbstractFirBasedSymbol<*>> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        explicitReceiver: ExpressionReceiverValue?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        val qualifiedReceiver = explicitReceiver?.explicitReceiverExpression as FirResolvedQualifier
        val classId = qualifiedReceiver.classId
        val scope = when {
            token == TowerScopeLevel.Token.Objects || classId == null -> {
                FirExplicitSimpleImportingScope(
                    listOf(
                        FirResolvedImportImpl(
                            FirImportImpl(null, FqName.topLevel(name), false, null),
                            qualifiedReceiver.packageFqName,
                            qualifiedReceiver.relativeClassFqName
                        )
                    ), session, bodyResolveComponents.scopeSession
                )
            }
            else -> {
                session.firSymbolProvider.getClassUseSiteMemberScope(classId, session, bodyResolveComponents.scopeSession)
                    ?: return ProcessorAction.NEXT
            }
        }

        val processorForCallables: (FirCallableSymbol<*>) -> ProcessorAction = {
            val fir = it.fir
            if (fir is FirCallableMemberDeclaration<*> && fir.isStatic ||
                it.callableId.classId == null ||
                fir is FirConstructor && !fir.isInner
            ) {
                @Suppress("UNCHECKED_CAST")
                processor.consumeCandidate(it as T, null, null)
            } else {
                ProcessorAction.NEXT
            }
        }

        return when (token) {
            TowerScopeLevel.Token.Objects -> scope.processClassifiersByName(name) {
                @Suppress("UNCHECKED_CAST")
                processor.consumeCandidate(it as T, null, null)
            }
            TowerScopeLevel.Token.Functions -> {
                scope.processFunctionsAndConstructorsByName(name, session, bodyResolveComponents, processorForCallables)
            }
            TowerScopeLevel.Token.Properties -> scope.processPropertiesByName(name, processorForCallables)

        }
    }
}

fun FirCallableDeclaration<*>.dispatchReceiverValue(session: FirSession): ClassDispatchReceiverValue? {
    // TODO: this is not true atCall least for inner class constructors
    if (this is FirConstructor) return null
    val id = this.symbol.callableId.classId ?: return null
    val symbol = session.firSymbolProvider.getClassLikeSymbolByFqName(id) as? FirClassSymbol ?: return null

    return ClassDispatchReceiverValue(symbol)
}

private fun FirCallableSymbol<*>.hasExtensionReceiver(): Boolean = this.fir.receiverTypeRef != null

private fun FirScope.processFunctionsAndConstructorsByName(
    name: Name,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    processor: (FirCallableSymbol<*>) -> ProcessorAction
): ProcessorAction {
    val matchedClassSymbol = getFirstClassifierOrNull(name) as? FirClassLikeSymbol<*>

    if (processConstructors(
            matchedClassSymbol,
            processor,
            session,
            bodyResolveComponents.scopeSession,
            name
        ).stop()
    ) {
        return ProcessorAction.STOP
    }

    if (processSyntheticConstructors(
            matchedClassSymbol,
            processor,
            bodyResolveComponents
        ).stop()
    ) {
        return ProcessorAction.STOP
    }

    return processFunctionsByName(name) {
        if (it is FirConstructorSymbol) ProcessorAction.NEXT
        else processor(it)
    }
}

private fun FirScope.getFirstClassifierOrNull(name: Name): FirClassifierSymbol<*>? {
    var result: FirClassifierSymbol<*>? = null
    processClassifiersByName(name) {
        result = it
        ProcessorAction.STOP
    }

    return result
}

private fun finalExpansionName(symbol: FirTypeAliasSymbol, session: FirSession): Name? {
    return when (val expandedType = symbol.fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>()) {
        is ConeAbbreviatedType ->
            expandedType.abbreviationLookupTag.toSymbol(session)?.safeAs<FirTypeAliasSymbol>()?.let {
                finalExpansionName(it, session)
            }
        else -> expandedType.lookupTag.classId.shortClassName
    }

}

val SAM_PARAMETER_NAME = Name.identifier("block")

private fun processSyntheticConstructors(
    matchedSymbol: FirClassLikeSymbol<*>?,
    processor: (FirFunctionSymbol<*>) -> ProcessorAction,
    bodyResolveComponents: BodyResolveComponents
): ProcessorAction {
    if (matchedSymbol == null) return ProcessorAction.NEXT
    if (matchedSymbol !is FirRegularClassSymbol) return ProcessorAction.NEXT

    val function = bodyResolveComponents.samResolver.getSamConstructor(matchedSymbol.fir) ?: return ProcessorAction.NEXT

    return processor(function.symbol)
}

private fun processConstructors(
    matchedSymbol: FirClassLikeSymbol<*>?,
    processor: (FirFunctionSymbol<*>) -> ProcessorAction,
    session: FirSession,
    scopeSession: ScopeSession,
    name: Name
): ProcessorAction {
    try {
        if (matchedSymbol != null) {
            val scope = when (matchedSymbol) {
                is FirTypeAliasSymbol -> matchedSymbol.fir.buildUseSiteMemberScope(session, scopeSession)
                is FirClassSymbol -> matchedSymbol.buildUseSiteMemberScope(session, scopeSession)
            }


            val constructorName = when (matchedSymbol) {
                is FirTypeAliasSymbol -> finalExpansionName(matchedSymbol, session) ?: return ProcessorAction.NEXT
                is FirClassSymbol -> name
            }

            //TODO: why don't we use declared member scope at this point?
            if (scope != null && scope.processFunctionsByName(
                    constructorName,
                    processor
                ) == ProcessorAction.STOP
            ) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    } catch (e: Throwable) {
        throw RuntimeException("While processing constructors", e)
    }
}
