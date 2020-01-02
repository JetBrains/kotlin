/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirResolvedCallableReferenceImpl(
    override val source: FirSourceElement?,
    override val name: Name,
    override val resolvedSymbol: AbstractFirBasedSymbol<*>
) : FirResolvedCallableReference() {
    override val candidateSymbol: AbstractFirBasedSymbol<*>? get() = null
    override val inferredTypeArguments: MutableList<ConeKotlinType> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirResolvedCallableReferenceImpl {
        return this
    }
}
