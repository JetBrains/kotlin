/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrAbstractVisitor

abstract class IrErrorDeclaration : IrDeclarationBase() {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitErrorDeclaration(this, data)

    override fun <R, D> accept(visitor: IrAbstractVisitor<R, D>, data: D): R =
        visitor.visitErrorDeclaration(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // no children
    }

    override fun <D> acceptChildren(visitor: IrAbstractVisitor<Unit, D>, data: D) {
        // no children
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // no children
    }

    override val symbol: IrSymbol
        get() = error("Should never be called")
}
