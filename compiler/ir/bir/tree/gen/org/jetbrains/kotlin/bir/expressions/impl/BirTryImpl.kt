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
import org.jetbrains.kotlin.bir.expressions.BirCatch
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirTry
import org.jetbrains.kotlin.bir.types.BirType

class BirTryImpl(
    override var sourceSpan: SourceSpan,
    override var type: BirType,
    tryResult: BirExpression,
    finallyExpression: BirExpression?,
) : BirTry() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _tryResult: BirExpression = tryResult

    override var tryResult: BirExpression
        get() = _tryResult
        set(value) {
            if (_tryResult != value) {
                replaceChild(_tryResult, value)
                _tryResult = value
            }
        }

    override val catches: BirChildElementList<BirCatch> = BirChildElementList(this, 0)

    private var _finallyExpression: BirExpression? = finallyExpression

    override var finallyExpression: BirExpression?
        get() = _finallyExpression
        set(value) {
            if (_finallyExpression != value) {
                replaceChild(_finallyExpression, value)
                _finallyExpression = value
            }
        }
    init {
        initChild(_tryResult)
        initChild(_finallyExpression)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._tryResult === old -> this.tryResult = new as BirExpression
            this._finallyExpression === old -> this.finallyExpression = new as BirExpression
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when {
        id == 0 -> this.catches
        else -> throwChildrenListWithIdNotFound(id)
    }
}
