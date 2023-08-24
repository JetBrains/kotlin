/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerShallow
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorShallow

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.branch]
 */
abstract class IrBranch : IrElementBase(), IrElement {
    abstract var condition: IrExpression

    abstract var result: IrExpression

    override fun <R, D> accept(visitor: IrElementVisitorShallow<R, D>, data: D): R =
        visitor.visitBranch(this, data)

    override fun <D> transform(transformer: IrElementTransformerShallow<D>, data: D):
            IrBranch = accept(transformer, data) as IrBranch

    override fun <D> acceptChildren(visitor: IrElementVisitorShallow<Unit, D>, data: D) {
        condition.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformerShallow<D>,
            data: D) {
        condition = condition.transform(transformer, data)
        result = result.transform(transformer, data)
    }
}
