/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirCatch
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirTry
import org.jetbrains.kotlin.bir.types.BirType

class BirTryImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    tryResult: BirExpression,
    finallyExpression: BirExpression?,
) : BirTry() {
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

    private var _tryResult: BirExpression? = tryResult

    override var tryResult: BirExpression
        get() {
            recordPropertyRead()
            return _tryResult ?: throwChildElementRemoved("tryResult")
        }
        set(value) {
            if (_tryResult != value) {
                replaceChild(_tryResult, value)
                _tryResult = value
                invalidate()
            }
        }

    override val catches: BirChildElementList<BirCatch> = BirChildElementList(this, 1, false)

    private var _finallyExpression: BirExpression? = finallyExpression

    override var finallyExpression: BirExpression?
        get() {
            recordPropertyRead()
            return _finallyExpression
        }
        set(value) {
            if (_finallyExpression != value) {
                replaceChild(_finallyExpression, value)
                _finallyExpression = value
                invalidate()
            }
        }
    init {
        initChild(_tryResult)
        initChild(_finallyExpression)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _tryResult?.acceptLite(visitor)
        catches.acceptChildrenLite(visitor)
        _finallyExpression?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._tryResult === old -> this.tryResult = new as BirExpression
            this._finallyExpression === old -> this.finallyExpression = new as BirExpression
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.catches
        else -> throwChildrenListWithIdNotFound(id)
    }
}
