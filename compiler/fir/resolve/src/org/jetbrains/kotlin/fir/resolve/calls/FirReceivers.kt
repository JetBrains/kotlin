/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.classId
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl

interface ReceiverValue {
    val type: ConeKotlinType
}

class ClassDispatchReceiverValue(val klassSymbol: FirClassSymbol) : ReceiverValue {
    override val type: ConeKotlinType = ConeClassTypeImpl(
        klassSymbol.toLookupTag(),
        klassSymbol.fir.typeParameters.map { ConeStarProjection }.toTypedArray(),
        isNullable = false
    )
}

class ExpressionReceiverValue(
    val explicitReceiverExpression: FirExpression,
    val typeProvider: (FirExpression) -> FirTypeRef?
) : ReceiverValue {
    override val type: ConeKotlinType
        get() = typeProvider(explicitReceiverExpression)?.coneTypeSafe()
            ?: ConeKotlinErrorType("No type calculated for: ${explicitReceiverExpression.renderWithType()}") // TODO: assert here
}

abstract class ImplicitReceiverValue(
    final override val type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession
) : ReceiverValue {
    val implicitScope: FirScope? = type.scope(useSiteSession, scopeSession)
}

class ImplicitDispatchReceiverValue(
    val boundSymbol: FirClassSymbol,
    type: ConeKotlinType,
    symbolProvider: FirSymbolProvider,
    useSiteSession: FirSession,
    scopeSession: ScopeSession
) : ImplicitReceiverValue(type, useSiteSession, scopeSession) {
    val implicitCompanionScope: FirScope? = boundSymbol.fir.companionObject?.let { companionObject ->
        symbolProvider.getClassUseSiteMemberScope(companionObject.classId, useSiteSession, scopeSession)
    }
}

class ImplicitExtensionReceiverValue(
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession
) : ImplicitReceiverValue(type, useSiteSession, scopeSession)
