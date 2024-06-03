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
import org.jetbrains.kotlin.bir.expressions.BirCatch
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirTry
import org.jetbrains.kotlin.bir.types.BirType

class BirTryImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    tryResult: BirExpression,
    finallyExpression: BirExpression?,
) : BirTry() {
    constructor(
        sourceSpan: SourceSpan,
        type: BirType,
        tryResult: BirExpression,
    ) : this(
        sourceSpan = sourceSpan,
        type = type,
        tryResult = tryResult,
        finallyExpression = null,
    )

    override var sourceSpan: SourceSpan = sourceSpan

    override var attributeOwnerId: BirAttributeContainer = this

    override var type: BirType = type

    private var _tryResult: BirExpression? = tryResult
    override var tryResult: BirExpression
        get() {
            return _tryResult ?: throwChildElementRemoved("tryResult")
        }
        set(value) {
            if (_tryResult !== value) {
                childReplaced(_tryResult, value)
                _tryResult = value
            }
        }

    private var _finallyExpression: BirExpression? = finallyExpression
    override var finallyExpression: BirExpression?
        get() {
            return _finallyExpression
        }
        set(value) {
            if (_finallyExpression !== value) {
                childReplaced(_finallyExpression, value)
                _finallyExpression = value
            }
        }

    override val catches: BirImplChildElementList<BirCatch> = BirImplChildElementList(this, 1, false)


    init {
        initChild(_tryResult)
        initChild(_finallyExpression)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _tryResult?.acceptLite(visitor)
        catches.acceptChildrenLite(visitor)
        _finallyExpression?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._tryResult === old -> {
                this._tryResult = new as BirExpression?
            }
            this._finallyExpression === old -> {
                this._finallyExpression = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.catches
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
