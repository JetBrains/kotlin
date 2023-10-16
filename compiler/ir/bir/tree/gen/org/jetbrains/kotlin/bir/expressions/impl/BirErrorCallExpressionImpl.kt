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
import org.jetbrains.kotlin.bir.expressions.BirErrorCallExpression
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.types.BirType

class BirErrorCallExpressionImpl(
    override var sourceSpan: SourceSpan,
    override var type: BirType,
    override var description: String,
    explicitReceiver: BirExpression?,
) : BirErrorCallExpression() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _explicitReceiver: BirExpression? = explicitReceiver

    override var explicitReceiver: BirExpression?
        get() = _explicitReceiver
        set(value) {
            if (_explicitReceiver != value) {
                replaceChild(_explicitReceiver, value)
                _explicitReceiver = value
            }
        }

    override val arguments: BirChildElementList<BirExpression> = BirChildElementList(this, 0)
    init {
        initChild(_explicitReceiver)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._explicitReceiver === old -> this.explicitReceiver = new as BirExpression
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when {
        id == 0 -> this.arguments
        else -> throwChildrenListWithIdNotFound(id)
    }
}
