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
import org.jetbrains.kotlin.bir.expressions.BirDoWhileLoop
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirDoWhileLoopImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    origin: IrStatementOrigin?,
    body: BirExpression?,
    condition: BirExpression,
    label: String?,
) : BirDoWhileLoop() {
    constructor(
        sourceSpan: SourceSpan,
        type: BirType,
        origin: IrStatementOrigin?,
        condition: BirExpression,
    ) : this(
        sourceSpan = sourceSpan,
        type = type,
        origin = origin,
        body = null,
        condition = condition,
        label = null,
    )

    override var sourceSpan: SourceSpan = sourceSpan

    override var attributeOwnerId: BirAttributeContainer = this

    override var type: BirType = type

    override var origin: IrStatementOrigin? = origin

    private var _body: BirExpression? = body
    override var body: BirExpression?
        get() {
            return _body
        }
        set(value) {
            if (_body !== value) {
                childReplaced(_body, value)
                _body = value
            }
        }

    private var _condition: BirExpression? = condition
    override var condition: BirExpression
        get() {
            return _condition ?: throwChildElementRemoved("condition")
        }
        set(value) {
            if (_condition !== value) {
                childReplaced(_condition, value)
                _condition = value
            }
        }

    override var label: String? = label


    init {
        initChild(_body)
        initChild(_condition)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _body?.acceptLite(visitor)
        _condition?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._body === old -> {
                this._body = new as BirExpression?
            }
            this._condition === old -> {
                this._condition = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
