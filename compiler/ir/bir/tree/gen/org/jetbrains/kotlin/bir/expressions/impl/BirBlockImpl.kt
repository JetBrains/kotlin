/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirStatement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirBlock
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirBlockImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    origin: IrStatementOrigin?,
) : BirBlock() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() = _sourceSpan
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this

    override var attributeOwnerId: BirAttributeContainer
        get() = _attributeOwnerId
        set(value) {
            if (_attributeOwnerId != value) {
                _attributeOwnerId = value
                invalidate()
            }
        }

    private var _type: BirType = type

    override var type: BirType
        get() = _type
        set(value) {
            if (_type != value) {
                _type = value
                invalidate()
            }
        }

    override val statements: BirChildElementList<BirStatement> = BirChildElementList(this, 0)

    private var _origin: IrStatementOrigin? = origin

    override var origin: IrStatementOrigin?
        get() = _origin
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        0 -> this.statements
        else -> throwChildrenListWithIdNotFound(id)
    }
}
