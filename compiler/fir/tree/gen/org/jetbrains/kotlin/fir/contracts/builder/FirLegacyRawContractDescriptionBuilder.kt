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
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirLegacyRawContractDescriptionImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall

@FirBuilderDsl
class FirLegacyRawContractDescriptionBuilder {
    var source: KtSourceElement? = null
    lateinit var contractCall: FirFunctionCall
    var diagnostic: ConeDiagnostic? = null

    fun build(): FirLegacyRawContractDescription {
        return FirLegacyRawContractDescriptionImpl(
            source,
            contractCall,
            diagnostic,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLegacyRawContractDescription(init: FirLegacyRawContractDescriptionBuilder.() -> Unit): FirLegacyRawContractDescription {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirLegacyRawContractDescriptionBuilder().apply(init).build()
}
