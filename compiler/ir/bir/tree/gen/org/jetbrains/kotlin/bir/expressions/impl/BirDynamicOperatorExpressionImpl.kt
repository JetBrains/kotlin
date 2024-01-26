/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirDynamicOperatorExpression
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator

class BirDynamicOperatorExpressionImpl(
    sourceSpan: CompressedSourceSpan,
    type: BirType,
    operator: IrDynamicOperator,
    receiver: BirExpression?,
) : BirDynamicOperatorExpression(BirDynamicOperatorExpression) {
    private var _sourceSpan: CompressedSourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan
        get() {
            recordPropertyRead()
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this
    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead()
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId !== value) {
                _attributeOwnerId = value
                invalidate()
            }
        }

    private var _type: BirType = type
    override var type: BirType
        get() {
            recordPropertyRead()
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate()
            }
        }

    private var _operator: IrDynamicOperator = operator
    override var operator: IrDynamicOperator
        get() {
            recordPropertyRead()
            return _operator
        }
        set(value) {
            if (_operator != value) {
                _operator = value
                invalidate()
            }
        }

    private var _receiver: BirExpression? = receiver
    override var receiver: BirExpression?
        get() {
            recordPropertyRead()
            return _receiver
        }
        set(value) {
            if (_receiver !== value) {
                childReplaced(_receiver, value)
                _receiver = value
                invalidate()
            }
        }

    override val arguments: BirImplChildElementList<BirExpression> = BirImplChildElementList(this, 1, false)

    init {
        initChild(_receiver)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _receiver?.acceptLite(visitor)
        arguments.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._receiver === old -> {
                this._receiver = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.arguments
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
