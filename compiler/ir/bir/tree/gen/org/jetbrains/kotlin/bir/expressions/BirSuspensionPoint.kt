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
import org.jetbrains.kotlin.bir.declarations.BirVariable

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.suspensionPoint]
 */
abstract class BirSuspensionPoint : BirExpression(), BirElement {
    abstract var suspensionPointIdParameter: BirVariable?
    abstract var result: BirExpression?
    abstract var resumeResult: BirExpression?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        suspensionPointIdParameter?.accept(data, visitor)
        result?.accept(data, visitor)
        resumeResult?.accept(data, visitor)
    }

    companion object : BirElementClass(BirSuspensionPoint::class.java, 86, true)
}
