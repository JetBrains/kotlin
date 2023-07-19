/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirConstructedClassTypeParameterRefImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirConstructedClassTypeParameterRefBuilder {
    var source: KtSourceElement? = null
    lateinit var symbol: FirTypeParameterSymbol

    fun build(): FirConstructedClassTypeParameterRef {
        return FirConstructedClassTypeParameterRefImpl(
            source,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructedClassTypeParameterRef(init: FirConstructedClassTypeParameterRefBuilder.() -> Unit): FirConstructedClassTypeParameterRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirConstructedClassTypeParameterRefBuilder().apply(init).build()
}
