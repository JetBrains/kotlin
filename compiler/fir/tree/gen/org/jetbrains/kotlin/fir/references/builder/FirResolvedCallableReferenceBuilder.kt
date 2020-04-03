/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.impl.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirResolvedCallableReferenceBuilder {
    var source: FirSourceElement? = null
    lateinit var name: Name
    lateinit var resolvedSymbol: AbstractFirBasedSymbol<*>
    val inferredTypeArguments: MutableList<ConeKotlinType> = mutableListOf()

    fun build(): FirResolvedCallableReference {
        return FirResolvedCallableReferenceImpl(
            source,
            name,
            resolvedSymbol,
            inferredTypeArguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedCallableReference(init: FirResolvedCallableReferenceBuilder.() -> Unit): FirResolvedCallableReference {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirResolvedCallableReferenceBuilder().apply(init).build()
}
