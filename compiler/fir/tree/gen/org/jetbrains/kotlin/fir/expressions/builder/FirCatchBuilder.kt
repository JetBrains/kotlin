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
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.impl.FirCatchImpl

@FirBuilderDsl
class FirCatchBuilder {
    var source: KtSourceElement? = null
    lateinit var parameter: FirProperty
    lateinit var block: FirBlock

    fun build(): FirCatch {
        return FirCatchImpl(
            source,
            parameter,
            block,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCatch(init: FirCatchBuilder.() -> Unit): FirCatch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirCatchBuilder().apply(init).build()
}
