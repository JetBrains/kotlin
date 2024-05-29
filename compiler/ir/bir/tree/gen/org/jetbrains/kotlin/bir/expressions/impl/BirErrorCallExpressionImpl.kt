/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirErrorCallExpression
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.types.BirType

class BirErrorCallExpressionImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    description: String,
    explicitReceiver: BirExpression?,
) : BirErrorCallExpression() {
    constructor(
        sourceSpan: SourceSpan,
        type: BirType,
        description: String,
    ) : this(
        sourceSpan = sourceSpan,
        type = type,
        description = description,
        explicitReceiver = null,
    )

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

    private var _description: String = description
    override var description: String
        get() {
            recordPropertyRead()
            return _description
        }
        set(value) {
            if (_description != value) {
                _description = value
                invalidate()
            }
        }

    private var _explicitReceiver: BirExpression? = explicitReceiver
    override var explicitReceiver: BirExpression?
        get() {
            recordPropertyRead()
            return _explicitReceiver
        }
        set(value) {
            if (_explicitReceiver !== value) {
                childReplaced(_explicitReceiver, value)
                _explicitReceiver = value
                invalidate()
            }
        }

    override val arguments: BirImplChildElementList<BirExpression> = BirImplChildElementList(this, 1, false)


    init {
        initChild(_explicitReceiver)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _explicitReceiver?.acceptLite(visitor)
        arguments.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._explicitReceiver === old -> {
                this._explicitReceiver = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.arguments
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
