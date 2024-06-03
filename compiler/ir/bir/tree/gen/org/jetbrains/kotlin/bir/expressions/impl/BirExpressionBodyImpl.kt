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
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody

class BirExpressionBodyImpl(
    sourceSpan: SourceSpan,
    expression: BirExpression,
) : BirExpressionBody() {
    override var sourceSpan: SourceSpan = sourceSpan

    private var _expression: BirExpression? = expression
    override var expression: BirExpression
        get() {
            return _expression ?: throwChildElementRemoved("expression")
        }
        set(value) {
            if (_expression !== value) {
                childReplaced(_expression, value)
                _expression = value
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
