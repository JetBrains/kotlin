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
import org.jetbrains.kotlin.fir.expressions.impl.FirLiteralExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf

fun buildLiteralExpression(
    source: KtSourceElement?,
    kind: ConstantValueKind,
    value: Any?,
    annotations: MutableList<FirAnnotation>? = null,
    setType: Boolean,
    prefix: String? = null,
): FirLiteralExpression {
    val coneType = runIf(setType) {
        kind.toConeType()
    }
    return FirLiteralExpressionImpl(source, coneType, annotations.toMutableOrEmpty(), kind, value, prefix)
}

fun ConstantValueKind.toConeType(): ConeKotlinType? = when (this) {
    ConstantValueKind.Boolean -> StandardTypes.Boolean
    ConstantValueKind.Byte -> StandardTypes.Byte
    ConstantValueKind.Char -> StandardTypes.Char
    ConstantValueKind.Double -> StandardTypes.Double
    ConstantValueKind.Float -> StandardTypes.Float
    ConstantValueKind.Int -> StandardTypes.Int
    ConstantValueKind.Long -> StandardTypes.Long
    ConstantValueKind.Null -> StandardTypes.NullableAny
    ConstantValueKind.Short -> StandardTypes.Short
    ConstantValueKind.String -> StandardTypes.String
    ConstantValueKind.UnsignedByte -> StandardTypes.UByte
    ConstantValueKind.UnsignedInt -> StandardTypes.UInt
    ConstantValueKind.UnsignedLong -> StandardTypes.ULong
    ConstantValueKind.UnsignedShort -> StandardTypes.UShort
    ConstantValueKind.IntegerLiteral,
    ConstantValueKind.UnsignedIntegerLiteral,
    ConstantValueKind.Error
        -> null
}