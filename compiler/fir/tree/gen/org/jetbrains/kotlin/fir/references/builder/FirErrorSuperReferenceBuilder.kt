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
import org.jetbrains.kotlin.fir.references.FirErrorSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirErrorSuperReferenceImpl
import org.jetbrains.kotlin.fir.types.FirTypeRef

@FirBuilderDsl
class FirErrorSuperReferenceBuilder {
    var source: KtSourceElement? = null
    var labelName: String? = null
    lateinit var superTypeRef: FirTypeRef
    lateinit var diagnostic: ConeDiagnostic

    fun build(): FirErrorSuperReference {
        return FirErrorSuperReferenceImpl(
            source,
            labelName,
            superTypeRef,
            diagnostic,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorSuperReference(init: FirErrorSuperReferenceBuilder.() -> Unit): FirErrorSuperReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirErrorSuperReferenceBuilder().apply(init).build()
}
