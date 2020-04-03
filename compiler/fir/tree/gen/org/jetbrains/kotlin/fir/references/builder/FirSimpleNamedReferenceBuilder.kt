/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirSimpleNamedReferenceBuilder {
    var source: FirSourceElement? = null
    lateinit var name: Name
    var candidateSymbol: AbstractFirBasedSymbol<*>? = null

    @OptIn(FirImplementationDetail::class)
    fun build(): FirNamedReference {
        return FirSimpleNamedReference(
            source,
            name,
            candidateSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSimpleNamedReference(init: FirSimpleNamedReferenceBuilder.() -> Unit): FirNamedReference {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirSimpleNamedReferenceBuilder().apply(init).build()
}
