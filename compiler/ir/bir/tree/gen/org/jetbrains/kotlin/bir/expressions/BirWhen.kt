/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.when]
 */
abstract class BirWhen : BirExpression() {
    abstract var origin: IrStatementOrigin?

    abstract val branches: MutableList<BirBranch>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        branches.forEach { it.accept(data, visitor) }
    }
}
