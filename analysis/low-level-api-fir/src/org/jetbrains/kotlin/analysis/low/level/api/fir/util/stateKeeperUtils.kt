/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyExpression

internal fun guardBlock(fir: FirBlock): FirBlock {
    return when (fir) {
        is FirLazyBlock -> fir
        else -> buildLazyBlock()
    }
}

internal fun guardExpression(fir: FirExpression): FirExpression {
    return when (fir) {
        is FirLazyExpression -> fir
        else -> buildLazyExpression { source = fir.source }
    }
}