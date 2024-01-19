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
import org.jetbrains.kotlin.name.StandardClassIds

class PrimitiveConeNumericComparisonInfo(
    val comparisonType: ConeClassLikeType,
    val leftType: ConeClassLikeType,
    val rightType: ConeClassLikeType
)

val FirComparisonExpression.left: FirExpression
    get() = compareToCall.explicitReceiver ?: error("There should be an explicit receiver for ${compareToCall.render()}")

val FirComparisonExpression.right: FirExpression
    get() = compareToCall.arguments.getOrNull(0) ?: error("There should be a first arg for ${compareToCall.render()}")

fun FirComparisonExpression.inferPrimitiveNumericComparisonInfo(): PrimitiveConeNumericComparisonInfo? =
    inferPrimitiveNumericComparisonInfo(left, right)

fun inferPrimitiveNumericComparisonInfo(left: FirExpression, right: FirExpression): PrimitiveConeNumericComparisonInfo? {
    val leftType = left.resolvedType
    val rightType = right.resolvedType
    val leftPrimitiveOrNullableType = leftType.getPrimitiveTypeOrSupertype() ?: return null
    val rightPrimitiveOrNullableType = rightType.getPrimitiveTypeOrSupertype() ?: return null
    val leastCommonType = leastCommonPrimitiveNumericType(leftPrimitiveOrNullableType, rightPrimitiveOrNullableType)

    return PrimitiveConeNumericComparisonInfo(leastCommonType, leftPrimitiveOrNullableType, rightPrimitiveOrNullableType)
}

private fun leastCommonPrimitiveNumericType(t1: ConeClassLikeType, t2: ConeClassLikeType): ConeClassLikeType {
    val pt1 = t1.promoteIntegerTypeToIntIfRequired()
    val pt2 = t2.promoteIntegerTypeToIntIfRequired()

    return when {
        pt1.isDouble() || pt2.isDouble() -> StandardTypes.Double
        pt1.isFloat() || pt2.isFloat() -> StandardTypes.Float
        pt1.isLong() || pt2.isLong() -> StandardTypes.Long
        pt1.isInt() || pt2.isInt() -> StandardTypes.Int
        else -> error("Unexpected types: t1=$t1, t2=$t2")
    }
}

private fun ConeClassLikeType.promoteIntegerTypeToIntIfRequired(): ConeClassLikeType =
    when (lookupTag.classId) {
        StandardClassIds.Byte, StandardClassIds.Short -> StandardTypes.Int
        StandardClassIds.Long, StandardClassIds.Int, StandardClassIds.Float, StandardClassIds.Double, StandardClassIds.Char -> this
        else -> error("Primitive number type expected: $this")
    }

private fun ConeKotlinType.getPrimitiveTypeOrSupertype(): ConeClassLikeType? =
    when {
        this is ConeTypeParameterType ->
            this.lookupTag.typeParameterSymbol.fir.bounds.firstNotNullOfOrNull {
                it.coneType.getPrimitiveTypeOrSupertype()
            }
        this is ConeClassLikeType && isPrimitiveNumberType() ->
            this
        this is ConeFlexibleType ->
            this.lowerBound.getPrimitiveTypeOrSupertype()
        else ->
            null
    }
