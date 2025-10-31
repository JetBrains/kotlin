/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildInaccessibleReceiverExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpression
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.smartcastScope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirThisOwnerSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.resolvedType

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
 * - [ImplicitReceiverValueForScriptOrSnippet] references a receiver in scripts
 * - [InaccessibleImplicitReceiverValue] references a dispatch receiver that's not available, e.g., in delegated constructor calls
 *
 * [ImplicitReceiverValue] is part of the [ImplicitValue] hierarchy, but [ExpressionReceiverValue] is **not**.
 *
 * See [ImplicitValue] KDoc for an explanation of its semantic.
 */
sealed interface ReceiverValue {
    val type: ConeKotlinType

    val receiverExpression: FirExpression

    fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? = type.scope(
        useSiteSession = useSiteSession,
        scopeSession = scopeSession,
        callableCopyTypeCalculator = CallableCopyTypeCalculator.DoNothing,
        requiredMembersPhase = FirResolvePhase.STATUS,
    )
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

sealed class ImplicitReceiverValue<S : FirThisOwnerSymbol<*>>(
    override val boundSymbol: S,
    type: ConeKotlinType,
    originalType: ConeKotlinType,
    val useSiteSession: FirSession,
    protected val scopeSession: ScopeSession,
    mutable: Boolean,
    private val inaccessibleReceiverKind: InaccessibleReceiverKind? = null,
) : ImplicitValue<S>(type, originalType, mutable), ReceiverValue {

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
        type.scope(
            useSiteSession,
            scopeSession,
            CallableCopyTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        )
    }

    override fun computeOriginalExpression(): FirExpression = receiverExpression(boundSymbol, originalType, inaccessibleReceiverKind)

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
    abstract fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitReceiverValue<S>
}

private fun receiverExpression(
    symbol: FirThisOwnerSymbol<*>,
    type: ConeKotlinType,
    inaccessibleReceiverKind: InaccessibleReceiverKind?,
): FirExpression {
    // NB: we can't use `symbol.fir.source` as the source of `this` receiver. For instance, if this is an implicit receiver for a class,
    // the entire class itself will be set as a source. If combined with an implicit type operation, a certain assertion, like null
    // check assertion, will retrieve source as an assertion message, which is literally the entire class (!).
    val calleeReference = buildImplicitThisReference {
        boundSymbol = symbol
    }
    val newSource = symbol.source?.fakeElement(KtFakeSourceElementKind.ImplicitThisReceiverExpression)
    return when (inaccessibleReceiverKind) {
        null -> buildThisReceiverExpression {
            source = newSource
            this.calleeReference = calleeReference
            this.coneTypeOrNull = type
            isImplicit = true
        }
        else -> buildInaccessibleReceiverExpression {
            source = newSource
            this.calleeReference = calleeReference
            this.coneTypeOrNull = type
            this.kind = inaccessibleReceiverKind
        }
    }
}

class ImplicitDispatchReceiverValue private constructor(
    boundSymbol: FirClassSymbol<*>,
    type: ConeKotlinType,
    originalType: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
) : ImplicitReceiverValue<FirClassSymbol<*>>(boundSymbol, type, originalType, useSiteSession, scopeSession, mutable) {
    constructor(
        boundSymbol: FirClassSymbol<*>,
        type: ConeKotlinType = boundSymbol.constructType(),
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
    ) : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true)

    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<FirClassSymbol<*>> {
        return ImplicitDispatchReceiverValue(boundSymbol, type, originalType, useSiteSession, scopeSession, keepMutable)
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitDispatchReceiverValue {
        return ImplicitDispatchReceiverValue(boundSymbol, type, originalType, newSession, newScopeSession, mutable)
    }
}

class ImplicitExtensionReceiverValue private constructor(
    boundSymbol: FirReceiverParameterSymbol,
    type: ConeKotlinType,
    originalType: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
) : ImplicitReceiverValue<FirReceiverParameterSymbol>(boundSymbol, type, originalType, useSiteSession, scopeSession, mutable) {
    constructor(boundSymbol: FirReceiverParameterSymbol, type: ConeKotlinType, useSiteSession: FirSession, scopeSession: ScopeSession)
            : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true)

    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<FirReceiverParameterSymbol> {
        return ImplicitExtensionReceiverValue(boundSymbol, type, originalType, useSiteSession, scopeSession, keepMutable)
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitExtensionReceiverValue {
        return ImplicitExtensionReceiverValue(boundSymbol, type, originalType, newSession, newScopeSession, mutable)
    }
}


class InaccessibleImplicitReceiverValue private constructor(
    boundSymbol: FirClassSymbol<*>,
    type: ConeKotlinType,
    originalType: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
    val kind: InaccessibleReceiverKind,
) : ImplicitReceiverValue<FirClassSymbol<*>>(
    boundSymbol, type, originalType, useSiteSession, scopeSession, mutable,
    inaccessibleReceiverKind = kind
) {
    constructor(
        boundSymbol: FirClassSymbol<*>,
        type: ConeKotlinType,
        kind: InaccessibleReceiverKind,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
    ) : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true, kind)

    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<FirClassSymbol<*>> {
        return InaccessibleImplicitReceiverValue(boundSymbol, type, originalType, useSiteSession, scopeSession, keepMutable, kind)
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): InaccessibleImplicitReceiverValue {
        return InaccessibleImplicitReceiverValue(boundSymbol, type, originalType, newSession, newScopeSession, mutable, kind)
    }
}

class ImplicitReceiverValueForScriptOrSnippet private constructor(
    boundSymbol: FirReceiverParameterSymbol,
    type: ConeKotlinType,
    originalType: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
) : ImplicitReceiverValue<FirReceiverParameterSymbol>(boundSymbol, type, originalType, useSiteSession, scopeSession, mutable) {

    constructor(boundSymbol: FirReceiverParameterSymbol, type: ConeKotlinType, useSiteSession: FirSession, scopeSession: ScopeSession)
            : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true)

    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<FirReceiverParameterSymbol> =
        ImplicitReceiverValueForScriptOrSnippet(boundSymbol, type, originalType, useSiteSession, scopeSession, keepMutable)

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): ImplicitReceiverValueForScriptOrSnippet {
        return ImplicitReceiverValueForScriptOrSnippet(boundSymbol, type, originalType, newSession, newScopeSession, mutable)
    }
}

val ImplicitReceiverValue<*>.referencedMemberSymbol: FirBasedSymbol<*>
    get() = when (val boundSymbol = boundSymbol) {
        is FirReceiverParameterSymbol -> boundSymbol.containingDeclarationSymbol
        else -> boundSymbol as FirBasedSymbol<*>
    }

fun ImplicitReceiverValue<*>.producesInapplicableCandidate(): Boolean {
    return this is InaccessibleImplicitReceiverValue && !this.kind.producesApplicableCandidate
}