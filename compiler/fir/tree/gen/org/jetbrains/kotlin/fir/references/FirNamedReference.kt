/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirNamedReference : FirReference() {
    abstract override val source: FirSourceElement?
    abstract val name: Name
    abstract val candidateSymbol: AbstractFirBasedSymbol<*>?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitNamedReference(this, data)
}
