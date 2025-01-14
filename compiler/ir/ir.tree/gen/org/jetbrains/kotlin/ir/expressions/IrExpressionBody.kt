/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.expressionBody]
 */
abstract class IrExpressionBody : IrBody() {
    abstract var expression: IrExpression

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitExpressionBody(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitExpressionBody(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrExpressionBody =
        transformer.visitExpressionBody(this, data) as IrExpressionBody

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpressionBody =
        transformer.visitExpressionBody(this) as IrExpressionBody

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        expression.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        expression = expression.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        expression = expression.transformVoid(transformer)
    }
}
