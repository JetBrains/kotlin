/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.dynamicMemberExpression]
 */
abstract class IrDynamicMemberExpression : IrDynamicExpression() {
    abstract var memberName: String

    abstract var receiver: IrExpression

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitDynamicMemberExpression(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitDynamicMemberExpression(this)
    }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrExpression =
        transformer.visitDynamicMemberExpression(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitDynamicMemberExpression(this)

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        receiver.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrVisitorVoid) {
        receiver.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        receiver = receiver.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        receiver = receiver.transformVoid(transformer)
    }
}
