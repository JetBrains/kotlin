/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirThisReference : FirReference() {
    abstract override val source: FirSourceElement?
    abstract val labelName: String?
    abstract val boundSymbol: FirBasedSymbol<*>?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitThisReference(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformThisReference(this, data) as E

    abstract fun replaceBoundSymbol(newBoundSymbol: FirBasedSymbol<*>?)
}
