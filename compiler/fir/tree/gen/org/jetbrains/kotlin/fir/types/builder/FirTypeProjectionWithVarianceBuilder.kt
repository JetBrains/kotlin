/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirTypeProjectionWithVarianceImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirTypeProjectionWithVarianceBuilder {
    var source: KtSourceElement? = null
    lateinit var typeRef: FirTypeRef
    lateinit var variance: Variance

    fun build(): FirTypeProjectionWithVariance {
        return FirTypeProjectionWithVarianceImpl(
            source,
            typeRef,
            variance,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeProjectionWithVariance(init: FirTypeProjectionWithVarianceBuilder.() -> Unit): FirTypeProjectionWithVariance {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirTypeProjectionWithVarianceBuilder().apply(init).build()
}
