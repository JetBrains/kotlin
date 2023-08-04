/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.references.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.impl.FirImplicitThisReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirImplicitThisReferenceBuilder {
    var boundSymbol: FirBasedSymbol<*>? = null
    var contextReceiverNumber: Int = -1
    var diagnostic: ConeDiagnostic? = null

    fun build(): FirThisReference {
        return FirImplicitThisReference(
            boundSymbol,
            contextReceiverNumber,
            diagnostic,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildImplicitThisReference(init: FirImplicitThisReferenceBuilder.() -> Unit = {}): FirThisReference {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirImplicitThisReferenceBuilder().apply(init).build()
}
