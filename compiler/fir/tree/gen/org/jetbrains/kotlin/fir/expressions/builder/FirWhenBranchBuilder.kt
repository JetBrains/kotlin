/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.impl.FirWhenBranchImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirWhenBranchBuilder {
    var source: KtSourceElement? = null
    lateinit var condition: FirExpression
    lateinit var result: FirBlock

    fun build(): FirWhenBranch {
        return FirWhenBranchImpl(
            source,
            condition,
            result,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildWhenBranch(init: FirWhenBranchBuilder.() -> Unit): FirWhenBranch {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirWhenBranchBuilder().apply(init).build()
}
