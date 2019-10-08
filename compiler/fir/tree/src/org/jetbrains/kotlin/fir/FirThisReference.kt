/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirThisReference : FirReference {
    val labelName: String?

    val boundSymbol: AbstractFirBasedSymbol<*>? get() = null

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitThisReference(this, data)
}