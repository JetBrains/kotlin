/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirSpreadElement

class BirSpreadElementImpl(
    override var sourceSpan: SourceSpan,
    expression: BirExpression,
) : BirSpreadElement() {
    private var _expression: BirExpression = expression

    override var expression: BirExpression
        get() = _expression
        set(value) {
            if (_expression != value) {
                replaceChild(_expression, value)
                _expression = value
            }
        }
    init {
        initChild(_expression)
    }
}
