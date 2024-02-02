/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirSuspensionPoint
import org.jetbrains.kotlin.bir.types.BirType

class BirSuspensionPointImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    suspensionPointIdParameter: BirVariable,
    result: BirExpression,
    resumeResult: BirExpression,
) : BirSuspensionPoint() {
    private var _sourceSpan: SourceSpan = sourceSpan
    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead()
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this
    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead()
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId !== value) {
                _attributeOwnerId = value
                invalidate()
            }
        }

    private var _type: BirType = type
    override var type: BirType
        get() {
            recordPropertyRead()
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate()
            }
        }

    private var _suspensionPointIdParameter: BirVariable? = suspensionPointIdParameter
    override var suspensionPointIdParameter: BirVariable
        get() {
            recordPropertyRead()
            return _suspensionPointIdParameter ?: throwChildElementRemoved("suspensionPointIdParameter")
        }
        set(value) {
            if (_suspensionPointIdParameter !== value) {
                childReplaced(_suspensionPointIdParameter, value)
                _suspensionPointIdParameter = value
                invalidate()
            }
        }

    private var _result: BirExpression? = result
    override var result: BirExpression
        get() {
            recordPropertyRead()
            return _result ?: throwChildElementRemoved("result")
        }
        set(value) {
            if (_result !== value) {
                childReplaced(_result, value)
                _result = value
                invalidate()
            }
        }

    private var _resumeResult: BirExpression? = resumeResult
    override var resumeResult: BirExpression
        get() {
            recordPropertyRead()
            return _resumeResult ?: throwChildElementRemoved("resumeResult")
        }
        set(value) {
            if (_resumeResult !== value) {
                childReplaced(_resumeResult, value)
                _resumeResult = value
                invalidate()
            }
        }


    init {
        initChild(_suspensionPointIdParameter)
        initChild(_result)
        initChild(_resumeResult)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _suspensionPointIdParameter?.acceptLite(visitor)
        _result?.acceptLite(visitor)
        _resumeResult?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._suspensionPointIdParameter === old -> {
                this._suspensionPointIdParameter = new as BirVariable?
            }
            this._result === old -> {
                this._result = new as BirExpression?
            }
            this._resumeResult === old -> {
                this._resumeResult = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
