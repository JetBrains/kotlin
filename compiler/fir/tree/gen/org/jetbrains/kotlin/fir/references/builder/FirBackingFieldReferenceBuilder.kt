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
import org.jetbrains.kotlin.fir.references.FirBackingFieldReference
import org.jetbrains.kotlin.fir.references.impl.FirBackingFieldReferenceImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol

@FirBuilderDsl
class FirBackingFieldReferenceBuilder {
    var source: KtSourceElement? = null
    lateinit var resolvedSymbol: FirBackingFieldSymbol

    fun build(): FirBackingFieldReference {
        return FirBackingFieldReferenceImpl(
            source,
            resolvedSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBackingFieldReference(init: FirBackingFieldReferenceBuilder.() -> Unit): FirBackingFieldReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirBackingFieldReferenceBuilder().apply(init).build()
}
