/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.references.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.impl.FirResolvedErrorReferenceImpl
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirResolvedErrorReferenceBuilder {
    var source: KtSourceElement? = null
    lateinit var name: Name
    lateinit var resolvedSymbol: FirBasedSymbol<*>
    lateinit var diagnostic: ConeDiagnostic

    fun build(): FirResolvedErrorReference {
        return FirResolvedErrorReferenceImpl(
            source,
            name,
            resolvedSymbol,
            diagnostic,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedErrorReference(init: FirResolvedErrorReferenceBuilder.() -> Unit): FirResolvedErrorReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirResolvedErrorReferenceBuilder().apply(init).build()
}
