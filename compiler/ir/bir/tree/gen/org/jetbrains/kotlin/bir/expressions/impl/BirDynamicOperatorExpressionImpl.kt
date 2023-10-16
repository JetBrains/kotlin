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
    override var sourceSpan: SourceSpan,
    override var originalBeforeInline: BirAttributeContainer?,
    override var type: BirType,
    override var operator: IrDynamicOperator,
    receiver: BirExpression,
) : BirDynamicOperatorExpression() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _receiver: BirExpression = receiver

    override var receiver: BirExpression
        get() = _receiver
        set(value) {
            if (_receiver != value) {
                replaceChild(_receiver, value)
                _receiver = value
            }
        }

    override val arguments: BirChildElementList<BirExpression> = BirChildElementList(this, 0)
    init {
        initChild(_receiver)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._receiver === old -> this.receiver = new as BirExpression
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when {
        id == 0 -> this.arguments
        else -> throwChildrenListWithIdNotFound(id)
    }
}
