/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.KtFakePsiSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.SuspiciousFakeSourceCheck
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildRawContractDescription
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyExpression

fun blockGuard(fir: FirBlock): FirBlock {
    if (isLazyStatement(fir)) {
        return fir
    }

    return buildLazyBlock()
}

fun expressionGuard(fir: FirExpression): FirExpression {
    if (isLazyStatement(fir)) {
        return fir
    }

    return buildLazyExpression {
        source = fir.source
    }
}

fun contractDescriptionGuard(fir: FirContractDescription): FirContractDescription = when (fir) {
    is FirRawContractDescription -> buildRawContractDescription {
        source = fir.source
        fir.rawEffects.mapTo(rawEffects) {
            buildLazyExpression { source = it.source }
        }
    }

    else -> fir
}

fun isLazyStatement(fir: FirStatement): Boolean {
    return fir is FirLazyExpression || fir is FirLazyBlock
}

val SPECIAL_BODY_CALLABLE_SOURCE_KINDS = setOf(
    KtFakeSourceElementKind.DefaultAccessor,
    KtFakeSourceElementKind.ImplicitConstructor,
    KtFakeSourceElementKind.PropertyFromParameter,
    KtFakeSourceElementKind.DataClassGeneratedMembers,
    KtFakeSourceElementKind.EnumGeneratedDeclaration,
)

@OptIn(SuspiciousFakeSourceCheck::class)
fun isCallableWithSpecialBody(fir: FirCallableDeclaration): Boolean {
    val source = fir.source as? KtFakePsiSourceElement ?: return false
    return source.kind in SPECIAL_BODY_CALLABLE_SOURCE_KINDS
}