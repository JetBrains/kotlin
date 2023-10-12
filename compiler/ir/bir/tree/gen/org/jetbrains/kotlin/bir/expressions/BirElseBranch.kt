/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.visitors.BirElementTransformer
import org.jetbrains.kotlin.bir.visitors.BirElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.elseBranch]
 */
abstract class BirElseBranch : BirBranch() {
    override fun <R, D> accept(visitor: BirElementVisitor<R, D>, data: D): R =
        visitor.visitElseBranch(this, data)

    override fun <D> transform(transformer: BirElementTransformer<D>, data: D):
            BirElseBranch = accept(transformer, data) as BirElseBranch
}
