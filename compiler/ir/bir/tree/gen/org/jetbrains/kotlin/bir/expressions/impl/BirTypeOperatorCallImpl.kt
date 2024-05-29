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
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirTypeOperatorCall
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator

class BirTypeOperatorCallImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    operator: IrTypeOperator,
    argument: BirExpression,
    typeOperand: BirType,
) : BirTypeOperatorCall() {
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

    private var _operator: IrTypeOperator = operator
    override var operator: IrTypeOperator
        get() {
            recordPropertyRead()
            return _operator
        }
        set(value) {
            if (_operator != value) {
                _operator = value
                invalidate()
            }
        }

    private var _argument: BirExpression? = argument
    override var argument: BirExpression
        get() {
            recordPropertyRead()
            return _argument ?: throwChildElementRemoved("argument")
        }
        set(value) {
            if (_argument !== value) {
                childReplaced(_argument, value)
                _argument = value
                invalidate()
            }
        }

    private var _typeOperand: BirType = typeOperand
    override var typeOperand: BirType
        get() {
            recordPropertyRead()
            return _typeOperand
        }
        set(value) {
            if (_typeOperand != value) {
                _typeOperand = value
                invalidate()
            }
        }


    init {
        initChild(_argument)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _argument?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._argument === old -> {
                this._argument = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
