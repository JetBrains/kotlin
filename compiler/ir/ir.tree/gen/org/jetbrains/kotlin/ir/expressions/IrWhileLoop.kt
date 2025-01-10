/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.whileLoop]
 */
abstract class IrWhileLoop : IrLoop() {
    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitWhileLoop(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitWhileLoop(this)
    }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrExpression =
        transformer.visitWhileLoop(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitWhileLoop(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        condition.accept(visitor, data)
        body?.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        condition.acceptVoid(visitor)
        body?.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        condition = condition.transform(transformer, data)
        body = body?.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        condition = condition.transformVoid(transformer)
        body = body?.transformVoid(transformer)
    }
}
