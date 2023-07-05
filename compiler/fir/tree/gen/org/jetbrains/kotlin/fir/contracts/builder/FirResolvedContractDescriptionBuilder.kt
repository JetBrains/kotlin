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
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirResolvedContractDescriptionImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirResolvedContractDescriptionBuilder {
    var source: KtSourceElement? = null
    val effects: MutableList<FirEffectDeclaration> = mutableListOf()
    val unresolvedEffects: MutableList<FirContractElementDeclaration> = mutableListOf()

    fun build(): FirResolvedContractDescription {
        return FirResolvedContractDescriptionImpl(
            source,
            effects,
            unresolvedEffects,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedContractDescription(init: FirResolvedContractDescriptionBuilder.() -> Unit = {}): FirResolvedContractDescription {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirResolvedContractDescriptionBuilder().apply(init).build()
}
