/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.suspensionPoint]
 */
abstract class IrSuspensionPoint : IrExpression() {
    abstract var suspensionPointIdParameter: IrVariable

    abstract var result: IrExpression

    abstract var resumeResult: IrExpression

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitSuspensionPoint(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitSuspensionPoint(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrExpression =
        transformer.visitSuspensionPoint(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitSuspensionPoint(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        suspensionPointIdParameter.accept(visitor, data)
        result.accept(visitor, data)
        resumeResult.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        suspensionPointIdParameter.acceptVoid(visitor)
        result.acceptVoid(visitor)
        resumeResult.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        suspensionPointIdParameter = suspensionPointIdParameter.transform(transformer, data) as IrVariable
        result = result.transform(transformer, data)
        resumeResult = resumeResult.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        suspensionPointIdParameter = suspensionPointIdParameter.transformVoid(transformer) as IrVariable
        result = result.transformVoid(transformer)
        resumeResult = resumeResult.transformVoid(transformer)
    }
}
