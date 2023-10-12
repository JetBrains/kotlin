/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementBase
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.visitors.BirElementTransformer
import org.jetbrains.kotlin.bir.visitors.BirElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.catch]
 */
abstract class BirCatch : BirElementBase(), BirElement {
    abstract var catchParameter: BirVariable

    abstract var result: BirExpression

    override fun <R, D> accept(visitor: BirElementVisitor<R, D>, data: D): R =
        visitor.visitCatch(this, data)

    override fun <D> transform(transformer: BirElementTransformer<D>, data: D): BirCatch =
        accept(transformer, data) as BirCatch

    override fun <D> acceptChildren(visitor: BirElementVisitor<Unit, D>, data: D) {
        catchParameter.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: BirElementTransformer<D>, data: D) {
        catchParameter = catchParameter.transform(transformer, data) as BirVariable
        result = result.transform(transformer, data)
    }
}
