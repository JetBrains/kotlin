/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.suspensionPoint]
 */
abstract class IrSuspensionPoint : IrExpression() {
    abstract var suspensionPointIdParameter: IrVariable

    abstract var result: IrExpression

    abstract var resumeResult: IrExpression

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSuspensionPoint(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        suspensionPointIdParameter.accept(visitor, data)
        result.accept(visitor, data)
        resumeResult.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        val suspensionPointIdParameter = this.suspensionPointIdParameter
        suspensionPointIdParameter.transform(transformer, data).let { new ->
            if (new !== suspensionPointIdParameter) {
                this.suspensionPointIdParameter = new as IrVariable
            }
        }
        val result = this.result
        result.transform(transformer, data).let { new ->
            if (new !== result) {
                this.result = new
            }
        }
        val resumeResult = this.resumeResult
        resumeResult.transform(transformer, data).let { new ->
            if (new !== resumeResult) {
                this.resumeResult = new
            }
        }
    }
}
