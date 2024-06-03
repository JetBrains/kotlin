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
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.expressions.BirFunctionExpression
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirFunctionExpressionImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    origin: IrStatementOrigin,
    function: BirSimpleFunction,
) : BirFunctionExpression() {
    override var sourceSpan: SourceSpan = sourceSpan

    override var attributeOwnerId: BirAttributeContainer = this

    override var type: BirType = type

    override var origin: IrStatementOrigin = origin

    private var _function: BirSimpleFunction? = function
    override var function: BirSimpleFunction
        get() {
            return _function ?: throwChildElementRemoved("function")
        }
        set(value) {
            if (_function !== value) {
                childReplaced(_function, value)
                _function = value
            }
        }


    init {
        initChild(_function)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _function?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._function === old -> {
                this._function = new as BirSimpleFunction?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
