/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class PrimitiveConeNumericComparisonInfo(
    val comparisonType: ConeKotlinType,
    val leftPrimitiveType: ConeClassLikeType,
    val rightPrimitiveType: ConeClassLikeType,
    val leftType: ConeKotlinType,
    val rightType: ConeKotlinType
)

val FirComparisonExpression.left: FirExpression
    get() = compareToCall.explicitReceiver ?: error("There should be an explicit receiver for ${compareToCall.render()}")

val FirComparisonExpression.right: FirExpression
    get() = compareToCall.arguments.getOrNull(0) ?: error("There should be a first arg for ${compareToCall.render()}")

fun FirComparisonExpression.inferPrimitiveNumericComparisonInfo(): PrimitiveConeNumericComparisonInfo? {
    val leftType = left.typeRef.coneTypeSafe<ConeKotlinType>() ?: return null
    val rightType = right.typeRef.coneTypeSafe<ConeKotlinType>() ?: return null
    val leftPrimitiveOrNullableType = leftType.getPrimitiveTypeOrSupertype() ?: return null
    val rightPrimitiveOrNullableType = rightType.getPrimitiveTypeOrSupertype() ?: return null
    val leftPrimitiveType = leftPrimitiveOrNullableType.withNullability(ConeNullability.NOT_NULL)
    val rightPrimitiveType = rightPrimitiveOrNullableType.withNullability(ConeNullability.NOT_NULL)

    // TODO: Support different types with coercion
    if (leftPrimitiveType != rightPrimitiveType) return null
    val leastCommonType = rightPrimitiveType

    return PrimitiveConeNumericComparisonInfo(
        leastCommonType,
        leftPrimitiveType, rightPrimitiveType,
        leftPrimitiveOrNullableType, rightPrimitiveOrNullableType
    )
}

private fun ConeKotlinType.getPrimitiveTypeOrSupertype(): ConeClassLikeType? =
    when {
        this is ConeTypeParameterType ->
            this.lookupTag.typeParameterSymbol.fir.bounds.firstNotNullResult {
                it.coneTypeSafe<ConeKotlinType>()?.getPrimitiveTypeOrSupertype()
            }
        this is ConeClassLikeType && isPrimitiveNumberType() ->
            this
        else ->
            null
    }
