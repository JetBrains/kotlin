/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirDynamicOperatorExpression
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator

class BirDynamicOperatorExpressionImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    operator: IrDynamicOperator,
    receiver: BirExpression,
) : BirDynamicOperatorExpression() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
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
            if (_attributeOwnerId != value) {
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

    private var _receiver: BirExpression = receiver

    override var receiver: BirExpression
        get() {
            recordPropertyRead()
            return _receiver
        }
        set(value) {
            if (_receiver != value) {
                replaceChild(_receiver, value)
                _receiver = value
                invalidate()
            }
        }

    override val arguments: BirChildElementList<BirExpression> = BirChildElementList(this, 1)
    init {
        initChild(_receiver)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._receiver === old -> this.receiver = new as BirExpression
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.arguments
        else -> throwChildrenListWithIdNotFound(id)
    }
}
