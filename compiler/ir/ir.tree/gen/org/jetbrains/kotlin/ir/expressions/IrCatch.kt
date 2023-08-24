/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerShallow
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorShallow

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.catch]
 */
abstract class IrCatch : IrElementBase(), IrElement {
    abstract var catchParameter: IrVariable

    abstract var result: IrExpression

    override fun <R, D> accept(visitor: IrElementVisitorShallow<R, D>, data: D): R =
        visitor.visitCatch(this, data)

    override fun <D> transform(transformer: IrElementTransformerShallow<D>, data: D):
            IrCatch = accept(transformer, data) as IrCatch

    override fun <D> acceptChildren(visitor: IrElementVisitorShallow<Unit, D>, data: D) {
        catchParameter.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformerShallow<D>,
            data: D) {
        catchParameter = catchParameter.transform(transformer, data) as IrVariable
        result = result.transform(transformer, data)
    }
}
