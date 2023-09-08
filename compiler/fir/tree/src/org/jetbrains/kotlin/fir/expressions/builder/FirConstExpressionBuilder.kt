/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirConstExpressionImpl
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind

@OptIn(UnresolvedExpressionTypeAccess::class)
fun <T> buildConstExpression(
    source: KtSourceElement?,
    kind: ConstantValueKind<T>,
    value: T,
    annotations: MutableList<FirAnnotation> = mutableListOf(),
    setType: Boolean
): FirConstExpression<T> {
    return FirConstExpressionImpl(source, null, annotations.toMutableOrEmpty(), kind, value).also {
        if (setType) {
            when (kind) {
                ConstantValueKind.Boolean -> it.coneTypeOrNull = StandardClassIds.Boolean.constructClassLikeType()
                ConstantValueKind.Byte -> it.coneTypeOrNull = StandardClassIds.Byte.constructClassLikeType()
                ConstantValueKind.Char -> it.coneTypeOrNull = StandardClassIds.Char.constructClassLikeType()
                ConstantValueKind.Double -> it.coneTypeOrNull = StandardClassIds.Double.constructClassLikeType()
                ConstantValueKind.Float -> it.coneTypeOrNull = StandardClassIds.Float.constructClassLikeType()
                ConstantValueKind.Int -> it.coneTypeOrNull = StandardClassIds.Int.constructClassLikeType()
                ConstantValueKind.Long -> it.coneTypeOrNull = StandardClassIds.Long.constructClassLikeType()
                ConstantValueKind.Null -> it.coneTypeOrNull = StandardClassIds.Any.constructClassLikeType(isNullable = true)
                ConstantValueKind.Short -> it.coneTypeOrNull = StandardClassIds.Short.constructClassLikeType()
                ConstantValueKind.String -> it.coneTypeOrNull = StandardClassIds.String.constructClassLikeType()
                ConstantValueKind.UnsignedByte -> it.coneTypeOrNull = StandardClassIds.UByte.constructClassLikeType()
                ConstantValueKind.UnsignedInt -> it.coneTypeOrNull = StandardClassIds.UInt.constructClassLikeType()
                ConstantValueKind.UnsignedLong -> it.coneTypeOrNull = StandardClassIds.ULong.constructClassLikeType()
                ConstantValueKind.UnsignedShort -> it.coneTypeOrNull = StandardClassIds.UShort.constructClassLikeType()
                ConstantValueKind.IntegerLiteral,
                ConstantValueKind.UnsignedIntegerLiteral,
                ConstantValueKind.Error,
                -> {
                }
            }
        }
    }
}
