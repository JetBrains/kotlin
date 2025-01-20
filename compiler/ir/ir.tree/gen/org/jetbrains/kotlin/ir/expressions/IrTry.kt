/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.try]
 */
abstract class IrTry : IrExpression() {
    abstract var tryResult: IrExpression

    abstract val catches: MutableList<IrCatch>

    abstract var finallyExpression: IrExpression?

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitTry(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitTry(this)
    }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrExpression =
        transformer.visitTry(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitTry(this)

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        tryResult.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyExpression?.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrVisitorVoid) {
        tryResult.acceptVoid(visitor)
        catches.forEach { it.acceptVoid(visitor) }
        finallyExpression?.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        tryResult = tryResult.transform(transformer, data)
        catches.transformInPlace(transformer, data)
        finallyExpression = finallyExpression?.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        tryResult = tryResult.transformVoid(transformer)
        catches.transformInPlace(transformer)
        finallyExpression = finallyExpression?.transformVoid(transformer)
    }
}
