/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.vararg]
 */
abstract class IrVararg : IrExpression() {
    abstract var varargElementType: IrType

    abstract val elements: MutableList<IrVarargElement>

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitVararg(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitVararg(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrExpression =
        transformer.visitVararg(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitVararg(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        elements.forEach { it.accept(visitor, data) }
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        elements.forEach { it.acceptVoid(visitor) }
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        elements.transformInPlace(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        elements.transformInPlace(transformer)
    }
}
