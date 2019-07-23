/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

@BaseTransformedType
interface FirNamedReference : FirReference {
    val name: Name

    val candidateSymbol: ConeSymbol? get() = null

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitNamedReference(this, data)
}