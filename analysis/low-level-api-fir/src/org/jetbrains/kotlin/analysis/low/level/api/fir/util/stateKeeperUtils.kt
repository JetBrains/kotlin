/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyExpression

internal fun blockGuard(fir: FirBlock): FirBlock {
    if (isLazyStatement(fir)) {
        return fir
    }

    return buildLazyBlock()
}

internal fun expressionGuard(fir: FirExpression): FirExpression {
    if (isLazyStatement(fir)) {
        return fir
    }

    return buildLazyExpression {
        source = fir.source
    }
}

private fun isLazyStatement(fir: FirStatement): Boolean {
    return fir is FirLazyExpression || fir is FirLazyBlock
}

private val SPECIAL_BODY_CALLABLE_SOURCE_KINDS = setOf(
    KtFakeSourceElementKind.DefaultAccessor,
    KtFakeSourceElementKind.DelegatedPropertyAccessor,
    KtFakeSourceElementKind.ImplicitConstructor,
    KtFakeSourceElementKind.PropertyFromParameter,
    KtFakeSourceElementKind.DataClassGeneratedMembers,
    KtFakeSourceElementKind.EnumGeneratedDeclaration,
)

internal fun isCallableWithSpecialBody(fir: FirCallableDeclaration): Boolean {
    val source = fir.source as? KtFakeSourceElement ?: return false
    return source.kind in SPECIAL_BODY_CALLABLE_SOURCE_KINDS
}