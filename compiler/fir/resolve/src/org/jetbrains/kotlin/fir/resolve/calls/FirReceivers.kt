/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpression
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId

interface Receiver {

}

interface ReceiverValue : Receiver {
    val type: ConeKotlinType

    val receiverExpression: FirExpression

    fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? =
        type.scope(useSiteSession, scopeSession)
}

private fun receiverExpression(symbol: AbstractFirBasedSymbol<*>, type: ConeKotlinType): FirThisReceiverExpression =
    buildThisReceiverExpression {
        calleeReference = buildImplicitThisReference {
            boundSymbol = symbol
        }
        typeRef = buildResolvedTypeRef {
            this.type = type
        }
    }

class ClassDispatchReceiverValue(klassSymbol: FirClassSymbol<*>) : ReceiverValue {
    override val type: ConeKotlinType = ConeClassLikeTypeImpl(
        klassSymbol.toLookupTag(),
        (klassSymbol.fir as? FirTypeParameterRefsOwner)?.typeParameters?.map { ConeStarProjection }?.toTypedArray().orEmpty(),
        isNullable = false
    )

    override val receiverExpression: FirExpression = receiverExpression(klassSymbol, type)
}

// TODO: should inherit just Receiver, not ReceiverValue
abstract class AbstractExplicitReceiver<E : FirExpression> : Receiver {
    abstract val explicitReceiver: FirExpression
}

abstract class AbstractExplicitReceiverValue<E : FirExpression> : AbstractExplicitReceiver<E>(), ReceiverValue {
    override val type: ConeKotlinType
        get() = explicitReceiver.typeRef.coneTypeSafe()
            ?: ConeKotlinErrorType("No type calculated for: ${explicitReceiver.renderWithType()}") // TODO: assert here

    override val receiverExpression: FirExpression
        get() = explicitReceiver
}

internal class ExpressionReceiverValue(
    override val explicitReceiver: FirExpression
) : AbstractExplicitReceiverValue<FirExpression>(), ReceiverValue

sealed class ImplicitReceiverValue<S : AbstractFirBasedSymbol<*>>(
    val boundSymbol: S,
    type: ConeKotlinType,
    protected val useSiteSession: FirSession,
    protected val scopeSession: ScopeSession
) : ReceiverValue {
    final override var type: ConeKotlinType = type
        private set

    var implicitScope: FirScope? = type.scope(useSiteSession, scopeSession)
        private set

    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? = implicitScope

    private val originalReceiverExpression: FirThisReceiverExpression = receiverExpression(boundSymbol, type)
    final override var receiverExpression: FirExpression = originalReceiverExpression
        private set

    /*
     * Should be called only in ImplicitReceiverStack
     */
    internal fun replaceType(type: ConeKotlinType) {
        if (type == this.type) return
        this.type = type
        receiverExpression = if (type == originalReceiverExpression.typeRef.coneTypeUnsafe()) {
            originalReceiverExpression
        } else {
            buildExpressionWithSmartcast {
                originalExpression = originalReceiverExpression
                typeRef = originalReceiverExpression.typeRef.resolvedTypeFromPrototype(type)
                typesFromSmartCast = listOf(type)
            }
        }
        implicitScope = type.scope(useSiteSession, scopeSession)
    }
}

internal enum class ImplicitDispatchReceiverKind {
    REGULAR,
    REGULAR_IN_DELEGATED,
    COMPANION,
    COMPANION_FROM_SUPERTYPE
}

class ImplicitDispatchReceiverValue internal constructor(
    boundSymbol: FirClassSymbol<*>,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    private val kind: ImplicitDispatchReceiverKind = ImplicitDispatchReceiverKind.REGULAR
) : ImplicitReceiverValue<FirClassSymbol<*>>(boundSymbol, type, useSiteSession, scopeSession) {
    internal constructor(
        boundSymbol: FirClassSymbol<*>, useSiteSession: FirSession, scopeSession: ScopeSession, kind: ImplicitDispatchReceiverKind
    ) : this(
        boundSymbol, boundSymbol.constructType(typeArguments = emptyArray(), isNullable = false),
        useSiteSession, scopeSession, kind
    )

    fun copyForDelegated(): ImplicitDispatchReceiverValue =
        ImplicitDispatchReceiverValue(boundSymbol, type, useSiteSession, scopeSession, ImplicitDispatchReceiverKind.REGULAR_IN_DELEGATED)

    val inDelegated: Boolean get() = kind == ImplicitDispatchReceiverKind.REGULAR_IN_DELEGATED

    val implicitCompanion: Boolean get() = kind != ImplicitDispatchReceiverKind.REGULAR

    val companionFromSupertype: Boolean get() = kind == ImplicitDispatchReceiverKind.COMPANION_FROM_SUPERTYPE
}

class ImplicitExtensionReceiverValue(
    boundSymbol: FirCallableSymbol<*>,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession
) : ImplicitReceiverValue<FirCallableSymbol<*>>(boundSymbol, type, useSiteSession, scopeSession)
