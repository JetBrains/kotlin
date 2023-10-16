/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.expressions.BirElseBranch
import org.jetbrains.kotlin.bir.expressions.BirExpression

class BirElseBranchImpl(
    override var sourceSpan: SourceSpan,
    condition: BirExpression,
    result: BirExpression,
) : BirElseBranch() {
    private var _condition: BirExpression = condition

    override var condition: BirExpression
        get() = _condition
        set(value) {
            if (_condition != value) {
                replaceChild(_condition, value)
                _condition = value
            }
        }

    private var _result: BirExpression = result

    override var result: BirExpression
        get() = _result
        set(value) {
            if (_result != value) {
                replaceChild(_result, value)
                _result = value
            }
        }
    init {
        initChild(_condition)
        initChild(_result)
    }
}
