/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.impl.FirContractDescriptionImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirContractDescriptionBuilder {
    var source: FirSourceElement? = null
    val effects: MutableList<ConeEffectDeclaration> = mutableListOf()

    fun build(): FirContractDescription {
        return FirContractDescriptionImpl(
            source,
            effects,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildContractDescription(init: FirContractDescriptionBuilder.() -> Unit = {}): FirContractDescription {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirContractDescriptionBuilder().apply(init).build()
}
