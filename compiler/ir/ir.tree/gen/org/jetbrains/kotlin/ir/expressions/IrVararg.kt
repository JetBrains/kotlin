/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.vararg]
 */
abstract class IrVararg : IrExpression() {
    abstract var varargElementType: IrType

    abstract val elements: MutableList<IrVarargElement>

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitVararg(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitVararg(this)
    }

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        elements.forEach { it.accept(visitor, data) }
    }

    override fun acceptChildrenVoid(visitor: IrVisitorVoid) {
        elements.forEach { it.acceptVoid(visitor) }
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        elements.transformInPlace(transformer, data)
    }
}
