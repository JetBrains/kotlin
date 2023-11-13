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
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.expressions.BirFunctionExpression
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirFunctionExpressionImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    origin: IrStatementOrigin,
    function: BirSimpleFunction?,
) : BirFunctionExpression() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(5)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(5)
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
            recordPropertyRead(3)
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate(3)
            }
        }

    private var _origin: IrStatementOrigin = origin

    override var origin: IrStatementOrigin
        get() {
            recordPropertyRead(4)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(4)
            }
        }

    private var _function: BirSimpleFunction? = function

    override var function: BirSimpleFunction?
        get() {
            recordPropertyRead(1)
            return _function
        }
        set(value) {
            if (_function != value) {
                childReplaced(_function, value)
                _function = value
                invalidate(1)
            }
        }
    init {
        initChild(_function)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _function?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int = when {
        this._function === old -> {
            this._function = new as BirSimpleFunction?
            1
        }
        else -> throwChildForReplacementNotFound(old)
    }
}
