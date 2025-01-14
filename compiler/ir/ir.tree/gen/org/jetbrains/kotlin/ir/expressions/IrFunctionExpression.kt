/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.functionExpression]
 */
abstract class IrFunctionExpression : IrExpression() {
    abstract var origin: IrStatementOrigin

    abstract var function: IrSimpleFunction

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitFunctionExpression(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitFunctionExpression(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrExpression =
        transformer.visitFunctionExpression(this, data) as IrExpression

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitFunctionExpression(this) as IrExpression

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        function.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        function.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        function = function.transform(transformer, data) as IrSimpleFunction
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        function = function.transformVoid(transformer) as IrSimpleFunction
    }
}
