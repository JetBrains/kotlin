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
import org.jetbrains.kotlin.bir.expressions.BirConst
import org.jetbrains.kotlin.bir.expressions.BirConstantPrimitive
import org.jetbrains.kotlin.bir.types.BirType

class BirConstantPrimitiveImpl(
    override var sourceSpan: SourceSpan,
    override var type: BirType,
    value: BirConst<*>,
) : BirConstantPrimitive() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _value: BirConst<*> = value

    override var value: BirConst<*>
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
            this._value === old -> this.value = new as BirConst<*>
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
