/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirSymbolOwner<E> : FirElement where E : FirSymbolOwner<E>, E : FirDeclaration {
    override val source: FirSourceElement?
    val symbol: FirBasedSymbol<E>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitSymbolOwner(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformSymbolOwner(this, data) as E
}
