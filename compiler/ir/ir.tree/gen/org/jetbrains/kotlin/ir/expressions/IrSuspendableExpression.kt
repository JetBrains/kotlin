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
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.suspendableExpression]
 */
abstract class IrSuspendableExpression : IrExpression() {
    abstract var suspensionPointId: IrExpression

    abstract var result: IrExpression

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitSuspendableExpression(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitSuspendableExpression(this)
    }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrExpression =
        transformer.visitSuspendableExpression(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitSuspendableExpression(this)

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        suspensionPointId.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrVisitorVoid) {
        suspensionPointId.acceptVoid(visitor)
        result.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        suspensionPointId = suspensionPointId.transform(transformer, data)
        result = result.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        suspensionPointId = suspensionPointId.transformVoid(transformer)
        result = result.transformVoid(transformer)
    }
}
