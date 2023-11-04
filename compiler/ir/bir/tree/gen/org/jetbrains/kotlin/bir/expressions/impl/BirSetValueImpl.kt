/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirValueDeclaration
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirSetValue
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirSetValueImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    symbol: BirValueDeclaration,
    origin: IrStatementOrigin?,
    value: BirExpression,
) : BirSetValue() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(6)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(6)
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this

    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead(2)
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId != value) {
                _attributeOwnerId = value
                invalidate(2)
            }
        }

    private var _type: BirType = type

    override var type: BirType
        get() {
            recordPropertyRead(4)
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate(4)
            }
        }

    private var _symbol: BirValueDeclaration = symbol

    override var symbol: BirValueDeclaration
        get() {
            recordPropertyRead(3)
            return _symbol
        }
        set(value) {
            if (_symbol != value) {
                _symbol = value
                invalidate(3)
            }
        }

    private var _origin: IrStatementOrigin? = origin

    override var origin: IrStatementOrigin?
        get() {
            recordPropertyRead(5)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(5)
            }
        }

    private var _value: BirExpression? = value

    override var value: BirExpression
        get() {
            recordPropertyRead(1)
            return _value ?: throwChildElementRemoved("value")
        }
        set(value) {
            if (_value != value) {
                childReplaced(_value, value)
                _value = value
                invalidate(1)
            }
        }
    init {
        initChild(_value)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _value?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._value === old -> this._value = new as BirExpression?
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
