/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun buildWhenBranch(hasGuard: Boolean, init: FirAbstractWhenBranchBuilder.() -> Unit): FirWhenBranch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val builder = if (hasGuard) FirGuardedWhenBranchBuilder() else FirRegularWhenBranchBuilder()
    return builder.apply(init).build()
}

fun buildWhenBranch(
    hasGuard: Boolean,
    source: KtSourceElement? = null,
    condition: FirExpression,
    result: FirBlock,
): FirWhenBranch {
    return if (hasGuard) {
        buildGuardedWhenBranch {
            this.source = source
            this.condition = condition
            this.result = result
        }
    } else {
        buildRegularWhenBranch {
            this.source = source
            this.condition = condition
            this.result = result
        }
    }
}