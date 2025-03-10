/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrCatchImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    override var startOffset: Int,
    override var endOffset: Int,
    catchParameter: IrVariable,
    override var origin: IrStatementOrigin?,
) : IrCatch() {
    override var attributeOwnerId: IrElement = this

    override var catchParameter: IrVariable = catchParameter
        set(value) {
            if (field !== value) {
                childReplaced(field, value)
                field = value
            }
        }

    override var result: IrExpression
        get() = _result
        set(value) {
            if (!::_result.isInitialized || _result !== value) {
                childReplaced(_result, value)
                _result = value
            }
        }

    lateinit var _result: IrExpression

    init {
        childInitialized(catchParameter)
    }
}
