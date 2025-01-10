/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.catch]
 */
abstract class IrCatch : IrElementBase(), IrElement {
    abstract var catchParameter: IrVariable

    abstract var result: IrExpression

    abstract var origin: IrStatementOrigin?

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitCatch(this, data)

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrCatch =
        accept(transformer, data) as IrCatch

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        catchParameter.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        catchParameter = catchParameter.transform(transformer, data) as IrVariable
        result = result.transform(transformer, data)
    }
}
