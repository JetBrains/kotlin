/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
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
    tryResult: BirExpression?,
    finallyExpression: BirExpression?,
) : BirTry(BirTry) {
    private var _sourceSpan: SourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(6)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(6)
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this
    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead(4)
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId != value) {
                _attributeOwnerId = value
                invalidate(4)
            }
        }

    private var _type: BirType = type
    override var type: BirType
        get() {
            recordPropertyRead(5)
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate(5)
            }
        }

    private var _tryResult: BirExpression? = tryResult
    override var tryResult: BirExpression?
        get() {
            recordPropertyRead(2)
            return _tryResult
        }
        set(value) {
            if (_tryResult != value) {
                childReplaced(_tryResult, value)
                _tryResult = value
                invalidate(2)
            }
        }

    private var _finallyExpression: BirExpression? = finallyExpression
    override var finallyExpression: BirExpression?
        get() {
            recordPropertyRead(3)
            return _finallyExpression
        }
        set(value) {
            if (_finallyExpression != value) {
                childReplaced(_finallyExpression, value)
                _finallyExpression = value
                invalidate(3)
            }
        }

    override val catches: BirImplChildElementList<BirCatch> = BirImplChildElementList(this, 1, false)

    init {
        initChild(_tryResult)
        initChild(_finallyExpression)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _tryResult?.acceptLite(visitor)
        catches.acceptChildrenLite(visitor)
        _finallyExpression?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
        return when {
            this._tryResult === old -> {
                this._tryResult = new as BirExpression?
                2
            }
            this._finallyExpression === old -> {
                this._finallyExpression = new as BirExpression?
                3
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.catches
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
