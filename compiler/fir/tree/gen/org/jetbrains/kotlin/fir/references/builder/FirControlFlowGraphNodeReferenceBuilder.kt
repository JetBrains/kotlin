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
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphNodeReference
import org.jetbrains.kotlin.fir.references.impl.FirControlFlowGraphNodeReferenceImpl

@FirBuilderDsl
class FirControlFlowGraphNodeReferenceBuilder {
    var source: KtSourceElement? = null

    fun build(): FirControlFlowGraphNodeReference {
        return FirControlFlowGraphNodeReferenceImpl(
            source,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildControlFlowGraphNodeReference(init: FirControlFlowGraphNodeReferenceBuilder.() -> Unit = {}): FirControlFlowGraphNodeReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirControlFlowGraphNodeReferenceBuilder().apply(init).build()
}
