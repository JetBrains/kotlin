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
    val leftType: ConeKotlinType,
    val rightType: ConeKotlinType
)

val FirComparisonExpression.left: FirExpression
    get() = compareToCall.explicitReceiver ?: error("There should be an explicit receiver for ${compareToCall.render()}")

val FirComparisonExpression.right: FirExpression
    get() = compareToCall.arguments.getOrNull(0) ?: error("There should be a first arg for ${compareToCall.render()}")

context(c: Fir2IrComponents)
fun FirComparisonExpression.inferPrimitiveNumericComparisonInfo(): PrimitiveConeNumericComparisonInfo? =
    inferPrimitiveNumericComparisonInfo(left, right)

context(c: Fir2IrComponents)
fun inferPrimitiveNumericComparisonInfo(
    left: FirExpression,
    right: FirExpression,
): PrimitiveConeNumericComparisonInfo? {
    val leftType = left.resolvedType
    val rightType = right.resolvedType
    val leftPrimitiveOrNullableType = leftType.getPrimitiveTypeOrSupertype() ?: return null
    val rightPrimitiveOrNullableType = rightType.getPrimitiveTypeOrSupertype() ?: return null
    val leastCommonType = leastCommonPrimitiveNumericType(leftPrimitiveOrNullableType, rightPrimitiveOrNullableType)

    return PrimitiveConeNumericComparisonInfo(leastCommonType, leftPrimitiveOrNullableType, rightPrimitiveOrNullableType)
}

private fun leastCommonPrimitiveNumericType(t1: ConeKotlinType, t2: ConeKotlinType): ConeClassLikeType {
    val pt1 = t1.promoteIntegerTypeToIntIfRequired().lowerBoundIfFlexible()
    val pt2 = t2.promoteIntegerTypeToIntIfRequired().lowerBoundIfFlexible()

    return when {
        pt1 !is ConeClassLikeType || pt2 !is ConeClassLikeType -> error("Unexpected types: t1=$t1, t2=$t2")
        pt1.isDouble() || pt2.isDouble() -> StandardTypes.Double
        pt1.isFloat() || pt2.isFloat() -> StandardTypes.Float
        pt1.isLong() || pt2.isLong() -> StandardTypes.Long
        pt1.isInt() || pt2.isInt() -> StandardTypes.Int
        else -> error("Unexpected types: t1=$t1, t2=$t2")
    }
}

private fun ConeKotlinType.promoteIntegerTypeToIntIfRequired(): ConeKotlinType =
    when (classId) {
        StandardClassIds.Byte, StandardClassIds.Short -> StandardTypes.Int
        StandardClassIds.Long, StandardClassIds.Int, StandardClassIds.Float, StandardClassIds.Double, StandardClassIds.Char -> this
        else -> error("Primitive number type expected: $this")
    }

context(c: Fir2IrComponents)
private fun ConeKotlinType.getPrimitiveTypeOrSupertype(): ConeKotlinType? =
    when {
        this is ConeTypeParameterType ->
            this.lookupTag.typeParameterSymbol.fir.bounds.firstNotNullOfOrNull {
                it.coneType.getPrimitiveTypeOrSupertype()
            }
        this is ConeClassLikeType && isPrimitiveNumberType() ->
            this
        this is ConeFlexibleType -> {
            if ((lowerBound as? ConeClassLikeType)?.isPrimitiveNumberType() == true) this
            else lowerBound.getPrimitiveTypeOrSupertype()
        }
        this is ConeCapturedType ->
            this.approximateForIrOrSelf().getPrimitiveTypeOrSupertype()
        else ->
            null
    }
