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
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.errorCallExpression]
 */
abstract class IrErrorCallExpression : IrErrorExpression() {
    abstract var explicitReceiver: IrExpression?

    abstract val arguments: MutableList<IrExpression>

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitErrorCallExpression(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitErrorCallExpression(this)
    }

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        explicitReceiver?.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun acceptChildrenVoid(visitor: IrVisitorVoid) {
        explicitReceiver?.acceptVoid(visitor)
        arguments.forEach { it.acceptVoid(visitor) }
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        explicitReceiver = explicitReceiver?.transform(transformer, data)
        arguments.transformInPlace(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        explicitReceiver = explicitReceiver?.transformVoid(transformer)
        arguments.transformInPlace(transformer)
    }
}
