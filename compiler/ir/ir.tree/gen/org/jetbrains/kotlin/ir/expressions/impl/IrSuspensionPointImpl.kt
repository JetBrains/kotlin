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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrSuspensionPointImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    override var startOffset: Int,
    override var endOffset: Int,
    override var type: IrType,
    suspensionPointIdParameter: IrVariable,
    result: IrExpression,
    resumeResult: IrExpression,
) : IrSuspensionPoint() {
    override var attributeOwnerId: IrElement = this

    override var suspensionPointIdParameter: IrVariable = suspensionPointIdParameter
        set(value) {
            if (field !== value) {
                childReplaced(field, value)
                field = value
            }
        }

    override var result: IrExpression = result
        set(value) {
            if (field !== value) {
                childReplaced(field, value)
                field = value
            }
        }

    override var resumeResult: IrExpression = resumeResult
        set(value) {
            if (field !== value) {
                childReplaced(field, value)
                field = value
            }
        }

    init {
        childInitialized(suspensionPointIdParameter)
        childInitialized(result)
        childInitialized(resumeResult)
    }
}
