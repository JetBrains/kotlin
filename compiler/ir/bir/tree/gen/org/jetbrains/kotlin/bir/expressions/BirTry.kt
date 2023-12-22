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
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.try]
 */
abstract class BirTry(elementClass: BirElementClass) : BirExpression(elementClass), BirElement {
    abstract var tryResult: BirExpression?
    abstract val catches: BirChildElementList<BirCatch>
    abstract var finallyExpression: BirExpression?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        tryResult?.accept(data, visitor)
        catches.acceptChildren(visitor, data)
        finallyExpression?.accept(data, visitor)
    }

    companion object : BirElementClass(BirTry::class.java, 58, true)
}
