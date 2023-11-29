/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.types.BirType

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.vararg]
 */
abstract class BirVararg : BirExpression(), BirElement {
    abstract var varargElementType: BirType
    abstract val elements: BirChildElementList<BirVarargElement>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        elements.acceptChildren(visitor, data)
    }

    companion object
}
