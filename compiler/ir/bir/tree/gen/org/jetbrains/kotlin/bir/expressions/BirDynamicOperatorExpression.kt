/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.util.transformInPlace
import org.jetbrains.kotlin.bir.visitors.BirElementTransformer
import org.jetbrains.kotlin.bir.visitors.BirElementVisitor
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.dynamicOperatorExpression]
 */
abstract class BirDynamicOperatorExpression : BirDynamicExpression() {
    abstract var operator: IrDynamicOperator

    abstract var receiver: BirExpression

    abstract val arguments: MutableList<BirExpression>

    override fun <R, D> accept(visitor: BirElementVisitor<R, D>, data: D): R =
        visitor.visitDynamicOperatorExpression(this, data)

    override fun <D> acceptChildren(visitor: BirElementVisitor<Unit, D>, data: D) {
        receiver.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: BirElementTransformer<D>, data: D) {
        receiver = receiver.transform(transformer, data)
        arguments.transformInPlace(transformer, data)
    }
}
