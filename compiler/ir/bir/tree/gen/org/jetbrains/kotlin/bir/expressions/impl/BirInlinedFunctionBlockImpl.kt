/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirStatement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirFunctionAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirInlinedFunctionBlock
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirInlinedFunctionBlockImpl(
    override var sourceSpan: SourceSpan,
    override var originalBeforeInline: BirAttributeContainer?,
    override var type: BirType,
    override var origin: IrStatementOrigin?,
    override var inlineCall: BirFunctionAccessExpression,
    override var inlinedElement: BirElement,
) : BirInlinedFunctionBlock() {
    override var attributeOwnerId: BirAttributeContainer = this

    override val statements: BirChildElementList<BirStatement> = BirChildElementList(this, 0)

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when {
        id == 0 -> this.statements
        else -> throwChildrenListWithIdNotFound(id)
    }
}
