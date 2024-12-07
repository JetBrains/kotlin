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
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference

@FirBuilderDsl
class FirExplicitThisReferenceBuilder {
    var source: KtSourceElement? = null
    var labelName: String? = null
    var diagnostic: ConeDiagnostic? = null

    fun build(): FirThisReference {
        return FirExplicitThisReference(
            source,
            labelName,
            diagnostic,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildExplicitThisReference(init: FirExplicitThisReferenceBuilder.() -> Unit = {}): FirThisReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirExplicitThisReferenceBuilder().apply(init).build()
}
