/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirPropertyAccessor : @VisitedSupertype FirFunction<FirPropertyAccessor> {
    val isGetter: Boolean

    val isSetter: Boolean get() = !isGetter

    val status: FirDeclarationStatus

    val visibility: Visibility get() = status.visibility

    override val symbol: FirPropertyAccessorSymbol

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitPropertyAccessor(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super.acceptChildren(visitor, data)
        status.accept(visitor, data)
    }
}