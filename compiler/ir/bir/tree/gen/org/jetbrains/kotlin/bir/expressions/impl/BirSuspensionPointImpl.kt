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
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirSuspensionPoint
import org.jetbrains.kotlin.bir.types.BirType

class BirSuspensionPointImpl(
    override var sourceSpan: SourceSpan,
    override var originalBeforeInline: BirAttributeContainer?,
    override var type: BirType,
    suspensionPointIdParameter: BirVariable,
    result: BirExpression,
    resumeResult: BirExpression,
) : BirSuspensionPoint() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _suspensionPointIdParameter: BirVariable = suspensionPointIdParameter

    override var suspensionPointIdParameter: BirVariable
        get() = _suspensionPointIdParameter
        set(value) {
            if (_suspensionPointIdParameter != value) {
                replaceChild(_suspensionPointIdParameter, value)
                _suspensionPointIdParameter = value
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

    private var _resumeResult: BirExpression = resumeResult

    override var resumeResult: BirExpression
        get() = _resumeResult
        set(value) {
            if (_resumeResult != value) {
                replaceChild(_resumeResult, value)
                _resumeResult = value
            }
        }
    init {
        initChild(_suspensionPointIdParameter)
        initChild(_result)
        initChild(_resumeResult)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._suspensionPointIdParameter === old -> this.suspensionPointIdParameter = new as
                BirVariable
            this._result === old -> this.result = new as BirExpression
            this._resumeResult === old -> this.resumeResult = new as BirExpression
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
