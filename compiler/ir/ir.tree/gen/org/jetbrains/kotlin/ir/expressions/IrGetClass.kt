/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.visitors.IrElementTransformerShallow
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorShallow

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.getClass]
 */
abstract class IrGetClass : IrExpression() {
    abstract var argument: IrExpression

    override fun <R, D> accept(visitor: IrElementVisitorShallow<R, D>, data: D): R =
        visitor.visitGetClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitorShallow<Unit, D>, data: D) {
        argument.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformerShallow<D>,
            data: D) {
        argument = argument.transform(transformer, data)
    }
}
