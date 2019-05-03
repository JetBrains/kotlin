/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.renderWithType
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
    private val explicitReceiverExpression: FirExpression,
    val typeProvider: (FirExpression) -> FirTypeRef?
) : ReceiverValue {
    override val type: ConeKotlinType
        get() = typeProvider(explicitReceiverExpression)?.coneTypeSafe()
            ?: ConeKotlinErrorType("No type calculated for: ${explicitReceiverExpression.renderWithType()}") // TODO: assert here
}

interface ImplicitReceiverValue : ReceiverValue {
}

class ImplicitDispatchReceiverValue(
    val boundSymbol: FirClassSymbol,
    override val type: ConeKotlinType
) : ImplicitReceiverValue

class ImplicitExtensionReceiverValue(
    override val type: ConeKotlinType
) : ImplicitReceiverValue
