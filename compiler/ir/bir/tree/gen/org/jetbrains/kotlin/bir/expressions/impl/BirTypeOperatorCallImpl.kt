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
import org.jetbrains.kotlin.bir.expressions.BirTypeOperatorCall
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator

class BirTypeOperatorCallImpl(
    override var sourceSpan: SourceSpan,
    override var originalBeforeInline: BirAttributeContainer?,
    override var type: BirType,
    override var operator: IrTypeOperator,
    argument: BirExpression,
    override var typeOperand: BirType,
) : BirTypeOperatorCall() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _argument: BirExpression = argument

    override var argument: BirExpression
        get() = _argument
        set(value) {
            if (_argument != value) {
                replaceChild(_argument, value)
                _argument = value
            }
        }
    init {
        initChild(_argument)
    }
}
