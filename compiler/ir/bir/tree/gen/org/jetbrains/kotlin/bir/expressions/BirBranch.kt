/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.*

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.branch]
 */
abstract class BirBranch(elementClass: BirElementClass<*>) : BirImplElementBase(elementClass), BirElement {
    abstract var condition: BirExpression?
    abstract var result: BirExpression?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        condition?.accept(data, visitor)
        result?.accept(data, visitor)
    }

    companion object : BirElementClass<BirBranch>(BirBranch::class.java, 4, true)
}
