/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

internal fun blockGuard(fir: FirBlock): FirBlock {
    if (isLazyStatement(fir)) {
        return fir
    }

    return buildLazyBlock()
}

internal fun expressionGuard(fir: FirExpression): FirExpression {
    // Expression references shouldn't be replaced since they belong to the snippet
    if (isLazyStatement(fir) || fir is FirReplExpressionReference) {
        return fir
    }

    return buildLazyExpression {
        source = fir.source
    }
}

internal fun contractDescriptionGuard(fir: FirContractDescription): FirContractDescription = when (fir) {
    is FirRawContractDescription -> buildRawContractDescription {
        source = fir.source
        fir.rawEffects.mapTo(rawEffects) {
            buildLazyExpression { source = it.source }
        }
    }

    else -> fir
}

private fun isLazyStatement(fir: FirStatement): Boolean {
    return fir is FirLazyExpression || fir is FirLazyBlock
}

@OptIn(SuspiciousFakeSourceCheck::class)
internal fun isCallableWithSpecialBody(fir: FirCallableDeclaration): Boolean {
    val sourceKind = (fir.source as? KtFakePsiSourceElement)?.kind ?: return false
    return when (sourceKind) {
        is KtFakeSourceElementKind.EnumGeneratedDeclaration,
        is KtFakeSourceElementKind.DefaultAccessor,
        KtFakeSourceElementKind.ImplicitConstructor,
        KtFakeSourceElementKind.PropertyFromParameter,
        is KtFakeSourceElementKind.DataClassGeneratedMembers
            -> true

        else -> false
    }
}
