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
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.constantPrimitive]
 */
abstract class IrConstantPrimitive : IrConstantValue() {
    abstract var value: IrConst

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitConstantPrimitive(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitConstantPrimitive(this)
    }

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        value.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrVisitorVoid) {
        value.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        value = value.transform(transformer, data) as IrConst
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        value = value.transformVoid(transformer) as IrConst
    }
}
