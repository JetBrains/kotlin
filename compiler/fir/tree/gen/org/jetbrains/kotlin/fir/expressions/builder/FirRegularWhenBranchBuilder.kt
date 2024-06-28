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
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.impl.FirRegularWhenBranch

@FirBuilderDsl
class FirRegularWhenBranchBuilder : FirAbstractWhenBranchBuilder {
    override var source: KtSourceElement? = null
    override lateinit var condition: FirExpression
    override lateinit var result: FirBlock

    override fun build(): FirWhenBranch {
        return FirRegularWhenBranch(
            source,
            condition,
            result,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildRegularWhenBranch(init: FirRegularWhenBranchBuilder.() -> Unit): FirWhenBranch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirRegularWhenBranchBuilder().apply(init).build()
}
