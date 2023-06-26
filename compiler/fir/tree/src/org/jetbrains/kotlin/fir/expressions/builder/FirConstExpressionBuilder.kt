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
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.types.ConstantValueKind

fun <T> buildConstExpression(
    source: KtSourceElement?,
    kind: ConstantValueKind<T>,
    value: T,
    annotations: MutableList<FirAnnotation> = mutableListOf(),
    setType: Boolean
): FirConstExpression<T> {
    return FirConstExpressionImpl(source, annotations.toMutableOrEmpty(), kind, value).also {
        if (setType) {
            when (kind) {
                ConstantValueKind.Boolean -> it.typeRef = FirImplicitBooleanTypeRef(null)
                ConstantValueKind.Byte -> it.typeRef = FirImplicitByteTypeRef(null)
                ConstantValueKind.Char -> it.typeRef = FirImplicitCharTypeRef(null)
                ConstantValueKind.Double -> it.typeRef = FirImplicitDoubleTypeRef(null)
                ConstantValueKind.Float -> it.typeRef = FirImplicitFloatTypeRef(null)
                ConstantValueKind.Int -> it.typeRef = FirImplicitIntTypeRef(null)
                ConstantValueKind.Long -> it.typeRef = FirImplicitLongTypeRef(null)
                ConstantValueKind.Null -> it.typeRef = FirImplicitNullableAnyTypeRef(null)
                ConstantValueKind.Short -> it.typeRef = FirImplicitShortTypeRef(null)
                ConstantValueKind.String -> it.typeRef = FirImplicitStringTypeRef(null)
                ConstantValueKind.UnsignedByte -> it.typeRef = FirImplicitUByteTypeRef(null)
                ConstantValueKind.UnsignedInt -> it.typeRef = FirImplicitUIntTypeRef(null)
                ConstantValueKind.UnsignedLong -> it.typeRef = FirImplicitULongTypeRef(null)
                ConstantValueKind.UnsignedShort -> it.typeRef = FirImplicitUShortTypeRef(null)
                ConstantValueKind.IntegerLiteral,
                ConstantValueKind.UnsignedIntegerLiteral,
                ConstantValueKind.Error,
                -> {
                }
            }
        }
    }
}
