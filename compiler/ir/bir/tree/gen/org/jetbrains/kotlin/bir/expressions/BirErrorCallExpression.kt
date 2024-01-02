/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.*

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.errorCallExpression]
 */
abstract class BirErrorCallExpression(elementClass: BirElementClass) : BirErrorExpression(elementClass), BirElement {
    abstract var explicitReceiver: BirExpression?
    abstract val arguments: BirChildElementList<BirExpression>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        explicitReceiver?.accept(data, visitor)
        arguments.acceptChildren(visitor, data)
    }

    companion object : BirElementClass(BirErrorCallExpression::class.java, 25, true)
}
