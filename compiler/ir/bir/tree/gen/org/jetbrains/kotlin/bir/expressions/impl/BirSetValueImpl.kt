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
import org.jetbrains.kotlin.bir.declarations.BirValueDeclaration
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirSetValue
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirSetValueImpl(
    override var sourceSpan: SourceSpan,
    override var type: BirType,
    override var symbol: BirValueDeclaration,
    override var origin: IrStatementOrigin?,
    value: BirExpression,
) : BirSetValue() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _value: BirExpression = value

    override var value: BirExpression
        get() = _value
        set(value) {
            if (_value != value) {
                replaceChild(_value, value)
                _value = value
            }
        }
    init {
        initChild(_value)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._value === old -> this.value = new as BirExpression
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
