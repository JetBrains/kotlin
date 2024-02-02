/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirFunctionAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirInlinedFunctionBlock
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirInlinedFunctionBlockImpl(
    sourceSpan: CompressedSourceSpan,
    type: BirType,
    origin: IrStatementOrigin?,
    inlineCall: BirFunctionAccessExpression,
    inlinedElement: BirElement,
) : BirInlinedFunctionBlock(BirInlinedFunctionBlock) {
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan = sourceSpan

    override var attributeOwnerId: BirAttributeContainer = this

    override var type: BirType = type

    override var origin: IrStatementOrigin? = origin

    override var inlineCall: BirFunctionAccessExpression = inlineCall

    override var inlinedElement: BirElement = inlinedElement

    override val statements: BirImplChildElementList<BirStatement> = BirImplChildElementList(this, 1, false)

    init {
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        statements.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.statements
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
