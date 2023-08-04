/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirConstExpressionImpl
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind

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
                ConstantValueKind.Boolean -> it.type = StandardClassIds.Boolean.constructClassLikeType()
                ConstantValueKind.Byte -> it.type = StandardClassIds.Byte.constructClassLikeType()
                ConstantValueKind.Char -> it.type = StandardClassIds.Char.constructClassLikeType()
                ConstantValueKind.Double -> it.type = StandardClassIds.Double.constructClassLikeType()
                ConstantValueKind.Float -> it.type = StandardClassIds.Float.constructClassLikeType()
                ConstantValueKind.Int -> it.type = StandardClassIds.Int.constructClassLikeType()
                ConstantValueKind.Long -> it.type = StandardClassIds.Long.constructClassLikeType()
                ConstantValueKind.Null -> it.type = StandardClassIds.Any.constructClassLikeType(isNullable = true)
                ConstantValueKind.Short -> it.type = StandardClassIds.Short.constructClassLikeType()
                ConstantValueKind.String -> it.type = StandardClassIds.String.constructClassLikeType()
                ConstantValueKind.UnsignedByte -> it.type = StandardClassIds.UByte.constructClassLikeType()
                ConstantValueKind.UnsignedInt -> it.type = StandardClassIds.UInt.constructClassLikeType()
                ConstantValueKind.UnsignedLong -> it.type = StandardClassIds.ULong.constructClassLikeType()
                ConstantValueKind.UnsignedShort -> it.type = StandardClassIds.UShort.constructClassLikeType()
                ConstantValueKind.IntegerLiteral,
                ConstantValueKind.UnsignedIntegerLiteral,
                ConstantValueKind.Error,
                -> {
                }
            }
        }
    }
}
