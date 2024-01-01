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
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.impl.FirEffectDeclarationImpl

@FirBuilderDsl
class FirEffectDeclarationBuilder {
    var source: KtSourceElement? = null
    lateinit var effect: ConeEffectDeclaration

    fun build(): FirEffectDeclaration {
        return FirEffectDeclarationImpl(
            source,
            effect,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildEffectDeclaration(init: FirEffectDeclarationBuilder.() -> Unit): FirEffectDeclaration {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirEffectDeclarationBuilder().apply(init).build()
}
