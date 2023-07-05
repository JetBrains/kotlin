/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.contracts.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.contracts.FirContractElementDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionElement
import org.jetbrains.kotlin.fir.contracts.impl.FirContractElementDeclarationImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirContractElementDeclarationBuilder {
    var source: KtSourceElement? = null
    lateinit var effect: ConeContractDescriptionElement

    fun build(): FirContractElementDeclaration {
        return FirContractElementDeclarationImpl(
            source,
            effect,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildContractElementDeclaration(init: FirContractElementDeclarationBuilder.() -> Unit): FirContractElementDeclaration {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirContractElementDeclarationBuilder().apply(init).build()
}
