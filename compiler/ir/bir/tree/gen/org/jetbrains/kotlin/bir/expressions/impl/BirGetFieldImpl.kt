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
import org.jetbrains.kotlin.bir.expressions.BirGetField
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirFieldSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirGetFieldImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    symbol: BirFieldSymbol,
    superQualifierSymbol: BirClassSymbol?,
    receiver: BirExpression?,
    origin: IrStatementOrigin?,
) : BirGetField() {
    constructor(
        sourceSpan: SourceSpan,
        type: BirType,
        symbol: BirFieldSymbol,
        superQualifierSymbol: BirClassSymbol?,
        origin: IrStatementOrigin?,
    ) : this(
        sourceSpan = sourceSpan,
        type = type,
        symbol = symbol,
        superQualifierSymbol = superQualifierSymbol,
        receiver = null,
        origin = origin,
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

    private var _symbol: BirFieldSymbol = symbol
    override var symbol: BirFieldSymbol
        get() {
            recordPropertyRead()
            return _symbol
        }
        set(value) {
            if (_symbol !== value) {
                _symbol = value
                invalidate()
            }
        }

    private var _superQualifierSymbol: BirClassSymbol? = superQualifierSymbol
    override var superQualifierSymbol: BirClassSymbol?
        get() {
            recordPropertyRead()
            return _superQualifierSymbol
        }
        set(value) {
            if (_superQualifierSymbol !== value) {
                _superQualifierSymbol = value
                invalidate()
            }
        }

    private var _receiver: BirExpression? = receiver
    override var receiver: BirExpression?
        get() {
            recordPropertyRead()
            return _receiver
        }
        set(value) {
            if (_receiver !== value) {
                childReplaced(_receiver, value)
                _receiver = value
                invalidate()
            }
        }

    private var _origin: IrStatementOrigin? = origin
    override var origin: IrStatementOrigin?
        get() {
            recordPropertyRead()
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }


    init {
        initChild(_receiver)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _receiver?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._receiver === old -> {
                this._receiver = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
