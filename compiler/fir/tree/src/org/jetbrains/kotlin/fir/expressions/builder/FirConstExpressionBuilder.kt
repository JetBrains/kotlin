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
    setType: Boolean = false
): FirConstExpression<T> {
    return FirConstExpressionImpl(source, annotations.toMutableOrEmpty(), kind, value).also {
        if (setType) {
            when (value) {
                null -> it.typeRef = FirImplicitNullableAnyTypeRef(null)
                is String -> it.typeRef = FirImplicitStringTypeRef(null)
                is Long -> it.typeRef = FirImplicitLongTypeRef(null)
                is Int -> it.typeRef = FirImplicitIntTypeRef(null)
                is Boolean -> it.typeRef = FirImplicitBooleanTypeRef(null)
                is Double -> it.typeRef = FirImplicitDoubleTypeRef(null)
                is Float -> it.typeRef = FirImplicitFloatTypeRef(null)
            }
        }
    }
}
