/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.references.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirResolvedNamedReferenceImpl
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirResolvedNamedReferenceBuilder {
    var source: KtSourceElement? = null
    lateinit var name: Name
    lateinit var resolvedSymbol: FirBasedSymbol<*>

    fun build(): FirResolvedNamedReference {
        return FirResolvedNamedReferenceImpl(
            source,
            name,
            resolvedSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedNamedReference(init: FirResolvedNamedReferenceBuilder.() -> Unit): FirResolvedNamedReference {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirResolvedNamedReferenceBuilder().apply(init).build()
}
