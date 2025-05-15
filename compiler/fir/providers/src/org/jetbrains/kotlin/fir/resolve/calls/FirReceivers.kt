/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildInaccessibleReceiverExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirStaticPhantomThisExpression
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.smartcastScope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirThisOwnerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.Name

/**
 * A type of value that is in scope and can be used as a dispatch or extension receiver of a qualified access expression.
 *
 * ### Implementors
 *
 * [ExpressionReceiverValue]: An explicit expression like a [FirQualifiedAccessExpression] or
 * [FirThisReceiverExpression] (explicitly written like `this` or `this@label`).
 *
 * [ImplicitReceiverValue]: An implicit [FirThisReceiverExpression]
 * - [ImplicitDispatchReceiverValue] references a dispatch receiver
 * - [ImplicitExtensionReceiverValue] references an extension receiver
 * - [ImplicitReceiverValueForScript] references a receiver in scripts
 * - [InaccessibleImplicitReceiverValue] references a dispatch receiver that's not available, e.g., in delegated constructor calls
 * - [ContextReceiverValue]: references a context receiver
 *
 * [ImplicitReceiverValue] is part of the [ImplicitValue] hierarchy, but [ExpressionReceiverValue] is **not**.
 *
 * See [ImplicitValue] KDoc for an explanation of its semantic.
 */
sealed interface ReceiverValue {
    val type: ConeKotlinType?

    val receiverExpression: FirExpression

    fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? = when (this) {
        is PhantomStaticThis -> this.toScope()
        else -> type?.scope(
            useSiteSession = useSiteSession,
            scopeSession = scopeSession,
            callableCopyTypeCalculator = CallableCopyTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        )
    }
}

class ExpressionReceiverValue(override val receiverExpression: FirExpression) : ReceiverValue {
    override val type: ConeKotlinType
        get() = receiverExpression.resolvedType

    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? {
        var receiverExpr: FirExpression? = receiverExpression
        // Unwrap `x!!` to `x` and use the resulted expression to derive receiver type. This is necessary so that smartcast types inside
        // `!!` is handled correctly.
        if (receiverExpr is FirCheckNotNullCall) {
            receiverExpr = receiverExpr.arguments.firstOrNull()
        }

        if (receiverExpr is FirSmartCastExpression) {
            return receiverExpr.smartcastScope(
                useSiteSession,
                scopeSession,
                requiredMembersPhase = FirResolvePhase.STATUS,
            )
        }

        return type.scope(
            useSiteSession,
            scopeSession,
            CallableCopyTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        )
    }
}

sealed interface ImplicitReceiver<S>: ReceiverValue where S : FirThisOwnerSymbol<*>, S : FirBasedSymbol<*> {
    val boundSymbol: S
    val isContextReceiver: Boolean
    val useSiteSession: FirSession
    val scopeSession: ScopeSession

    fun createSnapshot(keepMutable: Boolean): ImplicitReceiver<S>

    @DelicateScopeAPI
    fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitReceiver<S>
}

sealed class ImplicitReceiverValue<S>(
    override val boundSymbol: S,
    type: ConeKotlinType,
    override val useSiteSession: FirSession,
    override val scopeSession: ScopeSession,
    mutable: Boolean,
    private val inaccessibleReceiver: Boolean = false,
) : ImplicitReceiver<S>, ImplicitValue<S>(type, mutable), ReceiverValue
        where S : FirThisOwnerSymbol<*>, S : FirBasedSymbol<*> {

    val implicitScope: FirTypeScope?
        get() = lazyImplicitScope.value

    /**
     * This scope is lazy to avoid redundant computation in the case where this scope is unused.
     * This is especially the case for lazy resolution.
     *
     * KT-73900 is an example where computation during the class initialization leads to a visible performance
     * difference.
     * In particular, it is triggered by [createSnapshot].
     */
    private var lazyImplicitScope: Lazy<FirTypeScope?> = lazy(LazyThreadSafetyMode.PUBLICATION) {
        originalType.scope(
            useSiteSession,
            scopeSession,
            CallableCopyTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        )
    }

    override fun computeOriginalExpression(): FirExpression = receiverExpression(boundSymbol, originalType, inaccessibleReceiver)

    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? = implicitScope

    final override val receiverExpression: FirExpression
        get() = computeExpression()

    @ImplicitValueInternals
    override fun updateTypeFromSmartcast(type: ConeKotlinType) {
        super.updateTypeFromSmartcast(type)
        lazyImplicitScope = lazy(LazyThreadSafetyMode.PUBLICATION) {
            type.scope(
                useSiteSession = useSiteSession,
                scopeSession = scopeSession,
                callableCopyTypeCalculator = CallableCopyTypeCalculator.DoNothing,
                requiredMembersPhase = FirResolvePhase.STATUS,
            )
        }
    }

    abstract override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<S>

    @DelicateScopeAPI
    override abstract fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitReceiverValue<S>
}

private fun receiverExpression(
    symbol: FirThisOwnerSymbol<*>,
    type: ConeKotlinType,
    inaccessibleReceiver: Boolean
): FirExpression {
    // NB: we can't use `symbol.fir.source` as the source of `this` receiver. For instance, if this is an implicit receiver for a class,
    // the entire class itself will be set as a source. If combined with an implicit type operation, a certain assertion, like null
    // check assertion, will retrieve source as an assertion message, which is literally the entire class (!).
    val calleeReference = buildImplicitThisReference {
        boundSymbol = symbol
    }
    val newSource = symbol.source?.fakeElement(KtFakeSourceElementKind.ImplicitThisReceiverExpression)
    return when (inaccessibleReceiver) {
        false -> buildThisReceiverExpression {
            source = newSource
            this.calleeReference = calleeReference
            this.coneTypeOrNull = type
            isImplicit = true
        }
        true -> buildInaccessibleReceiverExpression {
            source = newSource
            this.calleeReference = calleeReference
            this.coneTypeOrNull = type
        }
    }
}

class ImplicitDispatchReceiverValue(
    boundSymbol: FirClassSymbol<*>,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
) : ImplicitReceiverValue<FirClassSymbol<*>>(boundSymbol, type, useSiteSession, scopeSession, mutable) {
    constructor(
        boundSymbol: FirClassSymbol<*>, useSiteSession: FirSession, scopeSession: ScopeSession
    ) : this(
        boundSymbol, boundSymbol.constructType(),
        useSiteSession, scopeSession
    )

    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<FirClassSymbol<*>> {
        return ImplicitDispatchReceiverValue(boundSymbol, type, useSiteSession, scopeSession, keepMutable)
    }

    override val isContextReceiver: Boolean
        get() = false

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitDispatchReceiverValue {
        return ImplicitDispatchReceiverValue(boundSymbol, type, newSession, newScopeSession, mutable)
    }
}

class ImplicitExtensionReceiverValue(
    boundSymbol: FirReceiverParameterSymbol,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
) : ImplicitReceiverValue<FirReceiverParameterSymbol>(boundSymbol, type, useSiteSession, scopeSession, mutable) {
    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<FirReceiverParameterSymbol> {
        return ImplicitExtensionReceiverValue(boundSymbol, type, useSiteSession, scopeSession, keepMutable)
    }

    override val isContextReceiver: Boolean
        get() = false

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitExtensionReceiverValue {
        return ImplicitExtensionReceiverValue(boundSymbol, type, newSession, newScopeSession, mutable)
    }
}


class InaccessibleImplicitReceiverValue(
    boundSymbol: FirClassSymbol<*>,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
) : ImplicitReceiverValue<FirClassSymbol<*>>(boundSymbol, type, useSiteSession, scopeSession, mutable, inaccessibleReceiver = true) {
    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<FirClassSymbol<*>> {
        return InaccessibleImplicitReceiverValue(boundSymbol, type, useSiteSession, scopeSession, keepMutable)
    }

    override val isContextReceiver: Boolean
        get() = false

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): InaccessibleImplicitReceiverValue {
        return InaccessibleImplicitReceiverValue(boundSymbol, type, newSession, newScopeSession, mutable)
    }
}

class ContextReceiverValue(
    boundSymbol: FirValueParameterSymbol,
    type: ConeKotlinType,
    val labelName: Name?,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
) : ImplicitReceiverValue<FirValueParameterSymbol>(
    boundSymbol, type, useSiteSession, scopeSession, mutable,
) {
    override fun createSnapshot(keepMutable: Boolean): ContextReceiverValue =
        ContextReceiverValue(boundSymbol, type, labelName, useSiteSession, scopeSession, keepMutable)

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ContextReceiverValue {
        return ContextReceiverValue(boundSymbol, type, labelName, newSession, newScopeSession, mutable)
    }

    override val isContextReceiver: Boolean
        get() = true
}

class ImplicitReceiverValueForScriptOrSnippet(
    boundSymbol: FirReceiverParameterSymbol,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
) : ImplicitReceiverValue<FirReceiverParameterSymbol>(boundSymbol, type, useSiteSession, scopeSession, mutable) {

    override val isContextReceiver: Boolean
        get() = false

    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<FirReceiverParameterSymbol> =
        ImplicitReceiverValueForScriptOrSnippet(boundSymbol, type, useSiteSession, scopeSession, keepMutable)

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitReceiverValueForScriptOrSnippet {
        return ImplicitReceiverValueForScriptOrSnippet(boundSymbol, type, newSession, newScopeSession, mutable)
    }
}

class PhantomStaticThis(
    val classSymbol: FirClassLikeSymbol<*>,
    override val useSiteSession: FirSession,
    override val scopeSession: ScopeSession,
) : ImplicitReceiver<FirClassLikeSymbol<*>> {
    override val boundSymbol: FirClassLikeSymbol<*> = classSymbol
    override val type: ConeKotlinType? = null
    override val receiverExpression: FirExpression =
        FirStaticPhantomThisExpression(classSymbol, useSiteSession.builtinTypes.unitType.coneType)
    override val isContextReceiver: Boolean = false
    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiver<FirClassLikeSymbol<*>> = this

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitReceiver<FirClassLikeSymbol<*>> =
        PhantomStaticThis(classSymbol, newSession, newScopeSession)

    fun toScope(): FirTypeScope? {
        val expanded = classSymbol.fullyExpandedClass(useSiteSession) ?: return null
        val scope =
            expanded.fir.scopeProvider.getStaticScope(expanded.fir, useSiteSession, scopeSession) ?: return null
        return StaticScope(scope, classSymbol != expanded)
    }

}

private class StaticScope(
    val scope: FirContainingNamesAwareScope,
    val throughTypeAlias: Boolean
): FirTypeScope() {
    override fun mayContainName(name: Name): Boolean = scope.mayContainName(name)
    override val scopeOwnerLookupNames: List<String> = scope.scopeOwnerLookupNames

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction,
    ): ProcessorAction = ProcessorAction.NONE

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction,
    ): ProcessorAction = ProcessorAction.NONE

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(
        newSession: FirSession,
        newScopeSession: ScopeSession,
    ): FirTypeScope? = scope.withReplacedSessionOrNull(newSession, newScopeSession)?.let {
        StaticScope(it, throughTypeAlias)
    }

    override fun getCallableNames(): Set<Name> = scope.getCallableNames()
    override fun getClassifierNames(): Set<Name> = scope.getClassifierNames()

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        if (throughTypeAlias) return
        scope.processClassifiersByNameWithSubstitution(name) { classifier, substitutor ->
            if ((classifier as? FirClassLikeSymbol<*>)?.isInner != true) processor(classifier, substitutor)
        }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        scope.processDeclaredConstructors(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        scope.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        scope.processPropertiesByName(name, processor)
    }
}

val ImplicitReceiverValue<*>.referencedMemberSymbol: FirBasedSymbol<*>
    get() = when (val boundSymbol = boundSymbol) {
        is FirReceiverParameterSymbol -> boundSymbol.containingDeclarationSymbol
        else -> boundSymbol as FirBasedSymbol<*>
    }
