/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirPropertyFromParameterResolvedNamedReferenceBuilder {
    var source: FirSourceElement? = null
    lateinit var name: Name
    lateinit var resolvedSymbol: AbstractFirBasedSymbol<*>

    @OptIn(FirImplementationDetail::class)
    fun build(): FirResolvedNamedReference {
        return FirPropertyFromParameterResolvedNamedReference(
            source,
            name,
            resolvedSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyFromParameterResolvedNamedReference(init: FirPropertyFromParameterResolvedNamedReferenceBuilder.() -> Unit): FirResolvedNamedReference {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirPropertyFromParameterResolvedNamedReferenceBuilder().apply(init).build()
}
