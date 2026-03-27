/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirAnonymousFunctionExpressionImpl

fun buildAnonymousFunctionExpression(
    source: KtSourceElement? = null,
    anonymousFunction: FirAnonymousFunction,
    isTrailingLambda: Boolean = false,
): FirAnonymousFunctionExpression {
    return FirAnonymousFunctionExpressionImpl(
        source,
        anonymousFunction,
        isTrailingLambda,
    )
}

fun buildAnonymousFunctionExpressionCopy(
    original: FirAnonymousFunctionExpression,
    source: KtSourceElement? = original.source,
    anonymousFunction: FirAnonymousFunction = original.anonymousFunction,
    isTrailingLambda: Boolean = original.isTrailingLambda,
): FirAnonymousFunctionExpression {
    return FirAnonymousFunctionExpressionImpl(
        source,
        anonymousFunction,
        isTrailingLambda,
    )
}
