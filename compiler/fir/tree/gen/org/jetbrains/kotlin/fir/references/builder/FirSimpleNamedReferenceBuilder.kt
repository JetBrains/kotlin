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
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirSimpleNamedReferenceBuilder {
    var source: KtSourceElement? = null
    lateinit var name: Name

    @OptIn(FirImplementationDetail::class)
    fun build(): FirNamedReference {
        return FirSimpleNamedReference(
            source,
            name,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSimpleNamedReference(init: FirSimpleNamedReferenceBuilder.() -> Unit): FirNamedReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirSimpleNamedReferenceBuilder().apply(init).build()
}
