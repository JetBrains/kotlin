/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirVariable : @VisitedSupertype FirDeclaration, FirTypedDeclaration, FirNamedDeclaration, FirStatement {
    val isVar: Boolean

    val isVal: Boolean
        get() = !isVar

    val initializer: FirExpression?

    val delegate: FirExpression?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitVariable(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        super<FirTypedDeclaration>.acceptChildren(visitor, data)
    }
}