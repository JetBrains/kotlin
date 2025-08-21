/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.spreadElement]
 */
abstract class IrSpreadElement : IrElementBase(), IrVarargElement {
    abstract var expression: IrExpression

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitSpreadElement(this, data)

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrSpreadElement =
        accept(transformer, data) as IrSpreadElement

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        expression = expression.transform(transformer, data)
    }
}
