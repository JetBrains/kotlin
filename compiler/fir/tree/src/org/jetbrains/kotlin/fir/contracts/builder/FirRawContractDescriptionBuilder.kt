/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.contracts.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirRawContractDescriptionImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression

@FirBuilderDsl
class FirRawContractDescriptionBuilder {
    var source: KtSourceElement? = null
    val rawEffects: MutableList<FirExpression> = mutableListOf()

    fun build(): FirRawContractDescription {
        return FirRawContractDescriptionImpl(
            source,
            rawEffects,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildRawContractDescription(init: FirRawContractDescriptionBuilder.() -> Unit = {}): FirRawContractDescription {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirRawContractDescriptionBuilder().apply(init).build()
}
