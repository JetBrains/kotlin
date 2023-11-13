/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.expressions.BirBranch
import org.jetbrains.kotlin.bir.expressions.BirExpression

class BirBranchImpl(
    sourceSpan: SourceSpan,
    condition: BirExpression?,
    result: BirExpression?,
) : BirBranch() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
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
            if (_condition != value) {
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
            if (_result != value) {
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

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int = when {
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
