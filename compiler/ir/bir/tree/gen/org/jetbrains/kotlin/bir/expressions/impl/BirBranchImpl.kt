/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.expressions.BirBranch
import org.jetbrains.kotlin.bir.expressions.BirExpression

class BirBranchImpl(
    sourceSpan: SourceSpan,
    condition: BirExpression,
    result: BirExpression,
) : BirBranch() {
    override var sourceSpan: SourceSpan = sourceSpan

    private var _condition: BirExpression? = condition
    override var condition: BirExpression
        get() {
            return _condition ?: throwChildElementRemoved("condition")
        }
        set(value) {
            if (_condition !== value) {
                childReplaced(_condition, value)
                _condition = value
            }
        }

    private var _result: BirExpression? = result
    override var result: BirExpression
        get() {
            return _result ?: throwChildElementRemoved("result")
        }
        set(value) {
            if (_result !== value) {
                childReplaced(_result, value)
                _result = value
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

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._condition === old -> {
                this._condition = new as BirExpression?
            }
            this._result === old -> {
                this._result = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
