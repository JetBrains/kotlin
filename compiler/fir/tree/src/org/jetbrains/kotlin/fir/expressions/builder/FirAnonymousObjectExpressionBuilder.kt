/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirAnonymousObjectExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType

fun buildAnonymousObjectExpression(
    source: KtSourceElement? = null,
    coneTypeOrNull: ConeKotlinType? = null,
    anonymousObject: FirAnonymousObject,
): FirAnonymousObjectExpression {
    return FirAnonymousObjectExpressionImpl(
        source,
        coneTypeOrNull,
        anonymousObject,
    )
}
