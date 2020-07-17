/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirOuterClassTypeParameterRefBuilder {
    var source: FirSourceElement? = null
    lateinit var symbol: FirTypeParameterSymbol

    fun build(): FirTypeParameterRef {
        return FirOuterClassTypeParameterRef(
            source,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildOuterClassTypeParameterRef(init: FirOuterClassTypeParameterRefBuilder.() -> Unit): FirTypeParameterRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirOuterClassTypeParameterRefBuilder().apply(init).build()
}
