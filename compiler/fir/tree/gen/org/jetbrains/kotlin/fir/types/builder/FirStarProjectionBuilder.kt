/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.types.FirStarProjection
import org.jetbrains.kotlin.fir.types.impl.FirStarProjectionImpl

@FirBuilderDsl
class FirStarProjectionBuilder {
    var source: KtSourceElement? = null

    fun build(): FirStarProjection {
        return FirStarProjectionImpl(
            source,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildStarProjection(init: FirStarProjectionBuilder.() -> Unit = {}): FirStarProjection {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirStarProjectionBuilder().apply(init).build()
}
