/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirSuspensionPoint
import org.jetbrains.kotlin.bir.types.BirType

class BirSuspensionPointImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    suspensionPointIdParameter: BirVariable?,
    result: BirExpression?,
    resumeResult: BirExpression?,
) : BirSuspensionPoint(BirSuspensionPoint) {
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
            if (_attributeOwnerId !== value) {
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

    private var _suspensionPointIdParameter: BirVariable? = suspensionPointIdParameter
    override var suspensionPointIdParameter: BirVariable?
        get() {
            recordPropertyRead(1)
            return _suspensionPointIdParameter
        }
        set(value) {
            if (_suspensionPointIdParameter !== value) {
                childReplaced(_suspensionPointIdParameter, value)
                _suspensionPointIdParameter = value
                invalidate(1)
            }
        }

    private var _result: BirExpression? = result
    override var result: BirExpression?
        get() {
            recordPropertyRead(2)
            return _result
        }
        set(value) {
            if (_result !== value) {
                childReplaced(_result, value)
                _result = value
                invalidate(2)
            }
        }

    private var _resumeResult: BirExpression? = resumeResult
    override var resumeResult: BirExpression?
        get() {
            recordPropertyRead(3)
            return _resumeResult
        }
        set(value) {
            if (_resumeResult !== value) {
                childReplaced(_resumeResult, value)
                _resumeResult = value
                invalidate(3)
            }
        }


    init {
        initChild(_suspensionPointIdParameter)
        initChild(_result)
        initChild(_resumeResult)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _suspensionPointIdParameter?.acceptLite(visitor)
        _result?.acceptLite(visitor)
        _resumeResult?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
        return when {
            this._suspensionPointIdParameter === old -> {
                this._suspensionPointIdParameter = new as BirVariable?
                1
            }
            this._result === old -> {
                this._result = new as BirExpression?
                2
            }
            this._resumeResult === old -> {
                this._resumeResult = new as BirExpression?
                3
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
