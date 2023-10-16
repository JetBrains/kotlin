/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.expressions.BirFunctionExpression
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirFunctionExpressionImpl(
    override var sourceSpan: SourceSpan,
    override var type: BirType,
    override var origin: IrStatementOrigin,
    function: BirSimpleFunction,
) : BirFunctionExpression() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _function: BirSimpleFunction = function

    override var function: BirSimpleFunction
        get() = _function
        set(value) {
            if (_function != value) {
                replaceChild(_function, value)
                _function = value
            }
        }
    init {
        initChild(_function)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._function === old -> this.function = new as BirSimpleFunction
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
