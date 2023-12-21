/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.constantArray]
 */
abstract class BirConstantArray : BirConstantValue(), BirElement {
    abstract val elements: BirChildElementList<BirConstantValue>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        elements.acceptChildren(visitor, data)
    }

    companion object : BirElementClass(BirConstantArray::class.java, 11, true)
}
