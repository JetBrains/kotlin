/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirWhileLoop
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirWhileLoopImpl(
    override var sourceSpan: SourceSpan,
    override var originalBeforeInline: BirAttributeContainer?,
    override var type: BirType,
    override var origin: IrStatementOrigin?,
    body: BirExpression?,
    condition: BirExpression,
    override var label: String?,
) : BirWhileLoop() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _body: BirExpression? = body

    override var body: BirExpression?
        get() = _body
        set(value) {
            if (_body != value) {
                replaceChild(_body, value)
                _body = value
            }
        }

    private var _condition: BirExpression = condition

    override var condition: BirExpression
        get() = _condition
        set(value) {
            if (_condition != value) {
                replaceChild(_condition, value)
                _condition = value
            }
        }
    init {
        initChild(_body)
        initChild(_condition)
    }
}
