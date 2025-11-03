/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirLiteralExpressionImpl
import org.jetbrains.kotlin.types.ConstantValueKind

@OptIn(UnresolvedExpressionTypeAccess::class)
fun buildLiteralExpression(
    source: KtSourceElement?,
    kind: ConstantValueKind,
    value: Any?,
    annotations: MutableList<FirAnnotation>? = null,
    setType: Boolean,
    prefix: String? = null,
): FirLiteralExpression {
    return FirLiteralExpressionImpl(source, null, annotations.toMutableOrEmpty(), kind, value, prefix).also {
        if (setType) {
            when (kind) {
                ConstantValueKind.Boolean -> it.coneTypeOrNull = StandardTypes.Boolean
                ConstantValueKind.Byte -> it.coneTypeOrNull = StandardTypes.Byte
                ConstantValueKind.Char -> it.coneTypeOrNull = StandardTypes.Char
                ConstantValueKind.Double -> it.coneTypeOrNull = StandardTypes.Double
                ConstantValueKind.Float -> it.coneTypeOrNull = StandardTypes.Float
                ConstantValueKind.Int -> it.coneTypeOrNull = StandardTypes.Int
                ConstantValueKind.Long -> it.coneTypeOrNull = StandardTypes.Long
                ConstantValueKind.Null -> it.coneTypeOrNull = StandardTypes.NullableAny
                ConstantValueKind.Short -> it.coneTypeOrNull = StandardTypes.Short
                ConstantValueKind.String -> it.coneTypeOrNull = StandardTypes.String
                ConstantValueKind.UnsignedByte -> it.coneTypeOrNull = StandardTypes.UByte
                ConstantValueKind.UnsignedInt -> it.coneTypeOrNull = StandardTypes.UInt
                ConstantValueKind.UnsignedLong -> it.coneTypeOrNull = StandardTypes.ULong
                ConstantValueKind.UnsignedShort -> it.coneTypeOrNull = StandardTypes.UShort
                ConstantValueKind.IntegerLiteral,
                ConstantValueKind.UnsignedIntegerLiteral,
                ConstantValueKind.Error,
                    -> {
                }
            }
        }
    }
}
