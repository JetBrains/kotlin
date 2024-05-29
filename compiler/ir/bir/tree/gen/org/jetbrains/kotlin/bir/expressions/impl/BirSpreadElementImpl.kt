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
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirSpreadElement

class BirSpreadElementImpl(
    sourceSpan: SourceSpan,
    expression: BirExpression,
) : BirSpreadElement() {
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

    private var _expression: BirExpression? = expression
    override var expression: BirExpression
        get() {
            recordPropertyRead()
            return _expression ?: throwChildElementRemoved("expression")
        }
        set(value) {
            if (_expression !== value) {
                childReplaced(_expression, value)
                _expression = value
                invalidate()
            }
        }


    init {
        initChild(_expression)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _expression?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._expression === old -> {
                this._expression = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
