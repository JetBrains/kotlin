/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.CompressedSourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.expressions.BirElseBranch
import org.jetbrains.kotlin.bir.expressions.BirExpression

class BirElseBranchImpl(
    sourceSpan: CompressedSourceSpan,
    condition: BirExpression?,
    result: BirExpression?,
) : BirElseBranch(BirElseBranch) {
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
            recordPropertyRead(3)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(3)
            }
        }

    private var _condition: BirExpression? = condition
    override var condition: BirExpression?
        get() {
            recordPropertyRead(1)
            return _condition
        }
        set(value) {
            if (_condition !== value) {
                childReplaced(_condition, value)
                _condition = value
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


    init {
        initChild(_condition)
        initChild(_result)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _condition?.acceptLite(visitor)
        _result?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
        return when {
            this._condition === old -> {
                this._condition = new as BirExpression?
                1
            }
            this._result === old -> {
                this._result = new as BirExpression?
                2
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
