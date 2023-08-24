/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.builder.buildInaccessibleReceiverExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpression
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.smartcastScope
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.SmartcastStability

abstract class ReceiverValue {
    abstract val type: ConeKotlinType

    abstract val receiverExpression: FirExpression

    open fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? = type.scope(
        useSiteSession = useSiteSession,
        scopeSession = scopeSession,
        fakeOverrideTypeCalculator = FakeOverrideTypeCalculator.DoNothing,
        requiredMembersPhase = FirResolvePhase.STATUS,
    )
}

class ExpressionReceiverValue(override val receiverExpression: FirExpression) : ReceiverValue() {
    override val type: ConeKotlinType
        // NB: safe cast is necessary here
        get() = receiverExpression.coneTypeSafe()
            ?: ConeErrorType(ConeIntermediateDiagnostic("No type calculated for: ${receiverExpression.renderWithType()}")) // TODO: assert here

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
            FakeOverrideTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        )
    }
}

sealed class ImplicitReceiverValue<S : FirBasedSymbol<*>>(
    val boundSymbol: S,
    type: ConeKotlinType,
    val useSiteSession: FirSession,
    protected val scopeSession: ScopeSession,
    private val mutable: Boolean,
    val contextReceiverNumber: Int = -1,
    private val inaccessibleReceiver: Boolean = false
) : ReceiverValue() {
    final override var type: ConeKotlinType = type
        private set

    abstract val isContextReceiver: Boolean

    val originalType: ConeKotlinType = type

    var implicitScope: FirTypeScope? =
        type.scope(
            useSiteSession,
            scopeSession,
            FakeOverrideTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS
        )
        private set

    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? = implicitScope

    private var receiverIsSmartcasted: Boolean = false
    private var originalReceiverExpression: FirExpression =
        receiverExpression(boundSymbol, type, contextReceiverNumber, inaccessibleReceiver)
    private var _receiverExpression: FirExpression? = null

    private fun computeReceiverExpression(): FirExpression {
        _receiverExpression?.let { return it }
        val actualReceiverExpression = if (receiverIsSmartcasted) {
            buildSmartCastExpression {
                originalExpression = originalReceiverExpression
                smartcastType = buildResolvedTypeRef {
                    source = originalReceiverExpression.source?.fakeElement(KtFakeSourceElementKind.SmartCastedTypeRef)
                    type = this@ImplicitReceiverValue.type
                }
                typesFromSmartCast = listOf(this@ImplicitReceiverValue.type)
                smartcastStability = SmartcastStability.STABLE_VALUE
                coneTypeOrNull = this@ImplicitReceiverValue.type
            }
        } else {
            originalReceiverExpression
        }
        _receiverExpression = actualReceiverExpression
        return actualReceiverExpression
    }

    /**
     * The idea of receiver expression for implicit receivers is following:
     *   - Implicit receivers are mutable because of smartcasts
     *   - Expression of implicit receiver may be used during call resolution and then stored for later. This implies necesserity
     *      to keep receiver expression independent of state of corresponding implicit value
     *   - In the same time we don't want to create new receiver expression for each access in sake of performance
     * All those statements lead to the current implementation:
     *   - original receiver expression (without smartcast) always stored inside receiver value and can not be changed (TODO: except builder inference)
     *   - we keep information about was there smartcast or not in [receiverIsSmartcasted] field
     *   - we cache computed receiver expression in [_receiverExpression] field
     *   - if type of receiver value was changed this cache is dropped
     */
    final override val receiverExpression: FirExpression
        get() = computeReceiverExpression()

    @RequiresOptIn
    annotation class ImplicitReceiverInternals

    @Deprecated(level = DeprecationLevel.ERROR, message = "Builder inference should not modify implicit receivers. KT-54708")
    fun updateTypeInBuilderInference(type: ConeKotlinType) {
        this.type = type
        originalReceiverExpression = receiverExpression(boundSymbol, type, contextReceiverNumber, inaccessibleReceiver)
        _receiverExpression = null
        implicitScope = type.scope(
            useSiteSession = useSiteSession,
            scopeSession = scopeSession,
            fakeOverrideTypeCalculator = FakeOverrideTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        )
    }

    /*
     * Should be called only in ImplicitReceiverStack
     */
    @ImplicitReceiverInternals
    fun updateTypeFromSmartcast(type: ConeKotlinType) {
        if (type == this.type) return
        if (!mutable) error("Cannot mutate an immutable ImplicitReceiverValue")
        this.type = type
        receiverIsSmartcasted = type != this.originalType
        _receiverExpression = null
        implicitScope = type.scope(
            useSiteSession = useSiteSession,
            scopeSession = scopeSession,
            fakeOverrideTypeCalculator = FakeOverrideTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        )
    }

    abstract fun createSnapshot(): ImplicitReceiverValue<S>
}

private fun receiverExpression(
    symbol: FirBasedSymbol<*>,
    type: ConeKotlinType,
    contextReceiverNumber: Int,
    inaccessibleReceiver: Boolean
): FirExpression {
    // NB: we can't use `symbol.fir.source` as the source of `this` receiver. For instance, if this is an implicit receiver for a class,
    // the entire class itself will be set as a source. If combined with an implicit type operation, a certain assertion, like null
    // check assertion, will retrieve source as an assertion message, which is literally the entire class (!).
    val calleeReference = buildImplicitThisReference {
        boundSymbol = symbol
        this.contextReceiverNumber = contextReceiverNumber
    }
    return when (inaccessibleReceiver) {
        false -> buildThisReceiverExpression {
            this.calleeReference = calleeReference
            this.coneTypeOrNull = type
            isImplicit = true
        }
        true -> buildInaccessibleReceiverExpression {
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
        boundSymbol, boundSymbol.constructType(typeArguments = emptyArray(), isNullable = false),
        useSiteSession, scopeSession
    )

    override fun createSnapshot(): ImplicitReceiverValue<FirClassSymbol<*>> {
        return ImplicitDispatchReceiverValue(boundSymbol, type, useSiteSession, scopeSession, false)
    }

    override val isContextReceiver: Boolean
        get() = false
}

class ImplicitExtensionReceiverValue(
    boundSymbol: FirCallableSymbol<*>,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
) : ImplicitReceiverValue<FirCallableSymbol<*>>(boundSymbol, type, useSiteSession, scopeSession, mutable) {
    override fun createSnapshot(): ImplicitReceiverValue<FirCallableSymbol<*>> {
        return ImplicitExtensionReceiverValue(boundSymbol, type, useSiteSession, scopeSession, false)
    }

    override val isContextReceiver: Boolean
        get() = false
}


class InaccessibleImplicitReceiverValue(
    boundSymbol: FirClassSymbol<*>,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
) : ImplicitReceiverValue<FirClassSymbol<*>>(boundSymbol, type, useSiteSession, scopeSession, mutable, inaccessibleReceiver = true) {
    override fun createSnapshot(): ImplicitReceiverValue<FirClassSymbol<*>> {
        return InaccessibleImplicitReceiverValue(boundSymbol, type, useSiteSession, scopeSession, false)
    }

    override val isContextReceiver: Boolean
        get() = false
}

sealed class ContextReceiverValue<S : FirBasedSymbol<*>>(
    boundSymbol: S,
    type: ConeKotlinType,
    val labelName: Name?,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
    contextReceiverNumber: Int,
) : ImplicitReceiverValue<S>(
    boundSymbol, type, useSiteSession, scopeSession, mutable, contextReceiverNumber,
) {
    abstract override fun createSnapshot(): ContextReceiverValue<S>

    override val isContextReceiver: Boolean
        get() = true
}

class ContextReceiverValueForCallable(
    boundSymbol: FirCallableSymbol<*>,
    type: ConeKotlinType,
    labelName: Name?,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
    contextReceiverNumber: Int,
) : ContextReceiverValue<FirCallableSymbol<*>>(
    boundSymbol, type, labelName, useSiteSession, scopeSession, mutable, contextReceiverNumber
) {
    override fun createSnapshot(): ContextReceiverValue<FirCallableSymbol<*>> =
        ContextReceiverValueForCallable(boundSymbol, type, labelName, useSiteSession, scopeSession, mutable = false, contextReceiverNumber)
}

class ContextReceiverValueForClass(
    boundSymbol: FirClassSymbol<*>,
    type: ConeKotlinType,
    labelName: Name?,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
    contextReceiverNumber: Int,
) : ContextReceiverValue<FirClassSymbol<*>>(
    boundSymbol, type, labelName, useSiteSession, scopeSession, mutable, contextReceiverNumber
) {
    override fun createSnapshot(): ContextReceiverValue<FirClassSymbol<*>> =
        ContextReceiverValueForClass(boundSymbol, type, labelName, useSiteSession, scopeSession, mutable = false, contextReceiverNumber)
}

class ImplicitReceiverValueForScript(
    boundSymbol: FirScriptSymbol,
    type: ConeKotlinType,
    labelName: Name?,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
    contextReceiverNumber: Int,
) : ContextReceiverValue<FirScriptSymbol>(
    boundSymbol, type, labelName, useSiteSession, scopeSession, mutable, contextReceiverNumber
) {
    override fun createSnapshot(): ContextReceiverValue<FirScriptSymbol> =
        ImplicitReceiverValueForScript(boundSymbol, type, labelName, useSiteSession, scopeSession, mutable = false, contextReceiverNumber)
}

