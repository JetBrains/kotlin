/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.suspendableExpression]
 */
abstract class IrSuspendableExpression : IrExpression() {
    abstract var suspensionPointId: IrExpression

    abstract var result: IrExpression

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitSuspendableExpression(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitSuspendableExpression(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrExpression =
        transformer.visitSuspendableExpression(this, data)

    override fun transformVoid(transformer: IrLeafTransformerVoid): IrExpression =
        transformer.visitSuspendableExpression(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        suspensionPointId.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        suspensionPointId.acceptVoid(visitor)
        result.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        suspensionPointId = suspensionPointId.transform(transformer, data)
        result = result.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrLeafTransformerVoid) {
        suspensionPointId = suspensionPointId.transformVoid(transformer)
        result = result.transformVoid(transformer)
    }
}
