/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.dynamicOperatorExpression]
 */
abstract class BirDynamicOperatorExpression : BirDynamicExpression() {
    abstract var operator: IrDynamicOperator

    abstract var receiver: BirExpression?

    abstract val arguments: BirChildElementList<BirExpression>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        receiver?.accept(data, visitor)
        arguments.acceptChildren(visitor, data)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        receiver?.acceptLite(visitor)
        arguments.acceptChildrenLite(visitor)
    }

    companion object
}
