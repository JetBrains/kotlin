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
import org.jetbrains.kotlin.bir.expressions.BirSetField
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirFieldSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirSetFieldImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    symbol: BirFieldSymbol,
    superQualifierSymbol: BirClassSymbol?,
    receiver: BirExpression?,
    origin: IrStatementOrigin?,
    value: BirExpression,
) : BirSetField() {
    constructor(
        sourceSpan: SourceSpan,
        type: BirType,
        symbol: BirFieldSymbol,
        superQualifierSymbol: BirClassSymbol?,
        origin: IrStatementOrigin?,
        value: BirExpression,
    ) : this(
        sourceSpan = sourceSpan,
        type = type,
        symbol = symbol,
        superQualifierSymbol = superQualifierSymbol,
        receiver = null,
        origin = origin,
        value = value,
    )

    override var sourceSpan: SourceSpan = sourceSpan

    override var attributeOwnerId: BirAttributeContainer = this

    override var type: BirType = type

    override var symbol: BirFieldSymbol = symbol

    override var superQualifierSymbol: BirClassSymbol? = superQualifierSymbol

    private var _receiver: BirExpression? = receiver
    override var receiver: BirExpression?
        get() {
            return _receiver
        }
        set(value) {
            if (_receiver !== value) {
                childReplaced(_receiver, value)
                _receiver = value
            }
        }

    override var origin: IrStatementOrigin? = origin

    private var _value: BirExpression? = value
    override var value: BirExpression
        get() {
            return _value ?: throwChildElementRemoved("value")
        }
        set(value) {
            if (_value !== value) {
                childReplaced(_value, value)
                _value = value
            }
        }


    init {
        initChild(_receiver)
        initChild(_value)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _receiver?.acceptLite(visitor)
        _value?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._receiver === old -> {
                this._receiver = new as BirExpression?
            }
            this._value === old -> {
                this._value = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
