/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.spreadElement]
 */
abstract class IrSpreadElement : IrElementBase(), IrVarargElement {
    abstract var expression: IrExpression

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitSpreadElement(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitSpreadElement(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrSpreadElement =
        transformer.visitSpreadElement(this, data)

    override fun transformVoid(transformer: IrLeafTransformerVoid): IrSpreadElement =
        transformer.visitSpreadElement(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        expression.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        expression = expression.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrLeafTransformerVoid) {
        expression = expression.transformVoid(transformer)
    }
}
