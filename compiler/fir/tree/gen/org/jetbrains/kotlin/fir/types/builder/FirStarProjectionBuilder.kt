/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.types.FirStarProjection
import org.jetbrains.kotlin.fir.types.impl.FirStarProjectionImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirStarProjectionBuilder {
    var source: FirSourceElement? = null

    fun build(): FirStarProjection {
        return FirStarProjectionImpl(
            source,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildStarProjection(init: FirStarProjectionBuilder.() -> Unit = {}): FirStarProjection {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirStarProjectionBuilder().apply(init).build()
}
