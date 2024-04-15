/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCompoundExpressionBranch
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirCompoundExpressionBranchImpl

@FirBuilderDsl
class FirCompoundExpressionBranchBuilder {
    var source: KtSourceElement? = null
    lateinit var condition: FirExpression
    lateinit var result: FirBlock

    fun build(): FirCompoundExpressionBranch {
        return FirCompoundExpressionBranchImpl(
            source,
            condition,
            result,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCompoundExpressionBranch(init: FirCompoundExpressionBranchBuilder.() -> Unit): FirCompoundExpressionBranch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirCompoundExpressionBranchBuilder().apply(init).build()
}
