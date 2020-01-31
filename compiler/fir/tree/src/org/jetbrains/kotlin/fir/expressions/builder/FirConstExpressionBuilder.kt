/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.expressions.impl.FirConstExpressionImpl

fun <T> buildConstExpression(
    source: FirSourceElement?,
    kind: FirConstKind<T>,
    value: T,
    annotations: MutableList<FirAnnotationCall> = mutableListOf(),
): FirConstExpression<T> {
    return FirConstExpressionImpl(source, annotations, kind, value)
}