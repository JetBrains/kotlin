/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.BirImplChildElementList
import org.jetbrains.kotlin.bir.BirStatement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirFunctionAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirInlinedFunctionBlock
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirInlinedFunctionBlockImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    origin: IrStatementOrigin?,
    inlineCall: BirFunctionAccessExpression,
    inlinedElement: BirElement,
) : BirInlinedFunctionBlock() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(7)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(7)
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
            recordPropertyRead(5)
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate(5)
            }
        }

    override val statements: BirImplChildElementList<BirStatement> =
            BirImplChildElementList(this, 1, false)

    private var _origin: IrStatementOrigin? = origin

    override var origin: IrStatementOrigin?
        get() {
            recordPropertyRead(6)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(6)
            }
        }

    private var _inlineCall: BirFunctionAccessExpression = inlineCall

    override var inlineCall: BirFunctionAccessExpression
        get() {
            recordPropertyRead(3)
            return _inlineCall
        }
        set(value) {
            if (_inlineCall != value) {
                _inlineCall = value
                invalidate(3)
            }
        }

    private var _inlinedElement: BirElement = inlinedElement

    override var inlinedElement: BirElement
        get() {
            recordPropertyRead(4)
            return _inlinedElement
        }
        set(value) {
            if (_inlinedElement != value) {
                _inlinedElement = value
                invalidate(4)
            }
        }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        statements.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int = when {
        else -> throwChildForReplacementNotFound(old)
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.statements
        else -> throwChildrenListWithIdNotFound(id)
    }
}
