/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.functionReference]
 */
abstract class IrFunctionReference : IrCallableReference<IrFunctionSymbol>() {
    abstract var reflectionTarget: IrFunctionSymbol?

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitFunctionReference(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitFunctionReference(this)
    }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrExpression =
        transformer.visitFunctionReference(this, data) as IrExpression

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitFunctionReference(this) as IrExpression
}
