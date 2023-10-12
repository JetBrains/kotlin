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

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.blockBody]
 */
abstract class BirBlockBody : BirBody(), BirStatementContainer {
    override fun <R, D> accept(visitor: BirElementVisitor<R, D>, data: D): R =
        visitor.visitBlockBody(this, data)

    override fun <D> acceptChildren(visitor: BirElementVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: BirElementTransformer<D>, data: D) {
        statements.transformInPlace(transformer, data)
    }
}
