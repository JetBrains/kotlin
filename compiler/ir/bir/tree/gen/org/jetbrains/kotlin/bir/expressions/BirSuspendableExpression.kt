/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.suspendableExpression]
 */
abstract class BirSuspendableExpression(elementClass: BirElementClass) : BirExpression(elementClass), BirElement {
    abstract var suspensionPointId: BirExpression?
    abstract var result: BirExpression?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        suspensionPointId?.accept(data, visitor)
        result?.accept(data, visitor)
    }

    companion object : BirElementClass(BirSuspendableExpression::class.java, 55, true)
}
