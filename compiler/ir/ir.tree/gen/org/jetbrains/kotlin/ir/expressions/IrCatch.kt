/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.catch]
 */
abstract class IrCatch : IrElementBase(), IrElement {
    abstract var catchParameter: IrVariable

    abstract var result: IrExpression

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitCatch(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrCatch {
        val new = accept(transformer, data)
        if (new === this)
             return this
        else
             return new as IrCatch
    }


    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        catchParameter.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        val catchParameter = this.catchParameter
        catchParameter.transform(transformer, data).let { new ->
            if (new !== catchParameter) {
                this.catchParameter = new as IrVariable
            }
        }
        val result = this.result
        result.transform(transformer, data).let { new ->
            if (new !== result) {
                this.result = new
            }
        }
    }
}
