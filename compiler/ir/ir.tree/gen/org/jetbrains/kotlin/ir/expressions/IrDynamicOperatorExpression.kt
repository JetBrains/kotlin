/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.dynamicOperatorExpression]
 */
abstract class IrDynamicOperatorExpression : IrDynamicExpression() {
    abstract var operator: IrDynamicOperator

    abstract var receiver: IrExpression

    abstract val arguments: MutableList<IrExpression>

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitDynamicOperatorExpression(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitDynamicOperatorExpression(this)
    }

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        receiver.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun acceptChildrenVoid(visitor: IrVisitorVoid) {
        receiver.acceptVoid(visitor)
        arguments.forEach { it.acceptVoid(visitor) }
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        receiver = receiver.transform(transformer, data)
        arguments.transformInPlace(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        receiver = receiver.transformVoid(transformer)
        arguments.transformInPlace(transformer)
    }
}
