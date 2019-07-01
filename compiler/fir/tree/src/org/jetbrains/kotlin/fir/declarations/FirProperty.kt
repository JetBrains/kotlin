/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// May be should not inherit FirVariable
@BaseTransformedType
interface FirProperty : @VisitedSupertype FirDeclaration, FirCallableMemberDeclaration, FirVariable, FirMemberDeclaration {
    val isConst: Boolean get() = status.isConst

    val isLateInit: Boolean get() = status.isLateInit

    override val isOverride: Boolean get() = status.isOverride

    // Should it be nullable or have some default?
    val getter: FirPropertyAccessor

    val setter: FirPropertyAccessor?

    val backingFieldSymbol: FirBackingFieldSymbol

    fun <D> transformChildrenWithoutAccessors(transformer: FirTransformer<D>, data: D)

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirCallableMemberDeclaration>.acceptChildren(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter.accept(visitor, data)
        setter?.accept(visitor, data)
    }
}