/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpression
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.smartcastScope
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.types.SmartcastStability

interface Receiver

interface ReceiverValue : Receiver {
    val type: ConeKotlinType

    val receiverExpression: FirExpression

    fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? =
        type.scope(useSiteSession, scopeSession, FakeOverrideTypeCalculator.DoNothing)
}

// TODO: should inherit just Receiver, not ReceiverValue
abstract class AbstractExplicitReceiver<E : FirExpression> : Receiver {
    abstract val explicitReceiver: FirExpression
}

abstract class AbstractExplicitReceiverValue<E : FirExpression> : AbstractExplicitReceiver<E>(), ReceiverValue {
    override val type: ConeKotlinType
        // NB: safe cast is necessary here
        get() = explicitReceiver.typeRef.coneTypeSafe()
            ?: ConeKotlinErrorType(ConeIntermediateDiagnostic("No type calculated for: ${explicitReceiver.renderWithType()}")) // TODO: assert here

    override val receiverExpression: FirExpression
        get() = explicitReceiver
}

open class ExpressionReceiverValue(
    override val explicitReceiver: FirExpression
) : AbstractExplicitReceiverValue<FirExpression>(), ReceiverValue {
    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? {
        var receiverExpr: FirExpression? = receiverExpression
        // Unwrap `x!!` to `x` and use the resulted expression to derive receiver type. This is necessary so that smartcast types inside
        // `!!` is handled correctly.
        if (receiverExpr is FirCheckNotNullCall) {
            receiverExpr = receiverExpr.arguments.firstOrNull()
        }
        if (receiverExpr is FirExpressionWithSmartcast) {
            return receiverExpr.smartcastScope(useSiteSession, scopeSession)
        }
        return type.scope(useSiteSession, scopeSession, FakeOverrideTypeCalculator.DoNothing)
    }
}

sealed class ImplicitReceiverValue<S : FirBasedSymbol<*>>(
    val boundSymbol: S,
    type: ConeKotlinType,
    protected val useSiteSession: FirSession,
    protected val scopeSession: ScopeSession,
    private val mutable: Boolean,
) : ReceiverValue {
    final override var type: ConeKotlinType = type
        private set

    val originalType: ConeKotlinType = type

    var implicitScope: FirTypeScope? = type.scope(useSiteSession, scopeSession, FakeOverrideTypeCalculator.DoNothing)
        private set

    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? = implicitScope

    private val originalReceiverExpression: FirThisReceiverExpression = receiverExpression(boundSymbol, type)
    final override var receiverExpression: FirExpression = originalReceiverExpression
        private set

    /*
     * Should be called only in ImplicitReceiverStack
     */
    fun replaceType(type: ConeKotlinType) {
        if (!mutable) throw IllegalStateException("Cannot mutate an immutable ImplicitReceiverValue")
        if (type == this.type) return
        this.type = type
        receiverExpression = if (type == originalReceiverExpression.typeRef.coneType) {
            originalReceiverExpression
        } else {
            buildExpressionWithSmartcast {
                originalExpression = originalReceiverExpression
                smartcastType = originalReceiverExpression.typeRef.resolvedTypeFromPrototype(type)
                typesFromSmartCast = listOf(type)
                smartcastStability = SmartcastStability.STABLE_VALUE
            }
        }
        implicitScope = type.scope(useSiteSession, scopeSession, FakeOverrideTypeCalculator.DoNothing)
    }

    abstract fun createSnapshot(): ImplicitReceiverValue<S>
}

private fun receiverExpression(symbol: FirBasedSymbol<*>, type: ConeKotlinType): FirThisReceiverExpression =
    buildThisReceiverExpression {
        // NB: we can't use `symbol.fir.source` as the source of `this` receiver. For instance, if this is an implicit receiver for a class,
        // the entire class itself will be set as a source. If combined with an implicit type operation, a certain assertion, like null
        // check assertion, will retrieve source as an assertion message, which is literally the entire class (!).
        calleeReference = buildImplicitThisReference {
            boundSymbol = symbol
        }
        typeRef = buildResolvedTypeRef {
            this.type = type
        }
        isImplicit = true
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
}


class InaccessibleImplicitReceiverValue(
    boundSymbol: FirClassSymbol<*>,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    mutable: Boolean = true,
) : ImplicitReceiverValue<FirClassSymbol<*>>(boundSymbol, type, useSiteSession, scopeSession, mutable) {
    override fun createSnapshot(): ImplicitReceiverValue<FirClassSymbol<*>> {
        return InaccessibleImplicitReceiverValue(boundSymbol, type, useSiteSession, scopeSession, false)
    }
}
