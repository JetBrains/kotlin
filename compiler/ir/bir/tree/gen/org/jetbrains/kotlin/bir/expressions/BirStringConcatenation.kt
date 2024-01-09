/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.stringConcatenation]
 */
abstract class BirStringConcatenation(elementClass: BirElementClass<*>) : BirExpression(elementClass), BirElement {
    abstract val arguments: BirChildElementList<BirExpression>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        arguments.acceptChildren(visitor, data)
    }

    companion object : BirElementClass<BirStringConcatenation>(BirStringConcatenation::class.java, 54, true)
}
