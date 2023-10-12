/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.visitors.BirElementTransformer
import org.jetbrains.kotlin.bir.visitors.BirElementVisitor
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.functionExpression]
 */
abstract class BirFunctionExpression : BirExpression() {
    abstract var origin: IrStatementOrigin

    abstract var function: BirSimpleFunction

    override fun <R, D> accept(visitor: BirElementVisitor<R, D>, data: D): R =
        visitor.visitFunctionExpression(this, data)

    override fun <D> acceptChildren(visitor: BirElementVisitor<Unit, D>, data: D) {
        function.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: BirElementTransformer<D>, data: D) {
        function = function.transform(transformer, data) as BirSimpleFunction
    }
}
