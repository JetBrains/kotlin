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
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirPropertyReference
import org.jetbrains.kotlin.bir.symbols.BirFieldSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirPropertyReferenceImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    dispatchReceiver: BirExpression?,
    extensionReceiver: BirExpression?,
    origin: IrStatementOrigin?,
    typeArguments: List<BirType?>,
    symbol: BirPropertySymbol,
    field: BirFieldSymbol?,
    getter: BirSimpleFunctionSymbol?,
    setter: BirSimpleFunctionSymbol?,
) : BirPropertyReference() {
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

    private var _dispatchReceiver: BirExpression? = dispatchReceiver
    override var dispatchReceiver: BirExpression?
        get() {
            recordPropertyRead()
            return _dispatchReceiver
        }
        set(value) {
            if (_dispatchReceiver !== value) {
                childReplaced(_dispatchReceiver, value)
                _dispatchReceiver = value
                invalidate()
            }
        }

    private var _extensionReceiver: BirExpression? = extensionReceiver
    override var extensionReceiver: BirExpression?
        get() {
            recordPropertyRead()
            return _extensionReceiver
        }
        set(value) {
            if (_extensionReceiver !== value) {
                childReplaced(_extensionReceiver, value)
                _extensionReceiver = value
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

    private var _typeArguments: List<BirType?> = typeArguments
    override var typeArguments: List<BirType?>
        get() {
            recordPropertyRead()
            return _typeArguments
        }
        set(value) {
            if (_typeArguments != value) {
                _typeArguments = value
                invalidate()
            }
        }

    private var _symbol: BirPropertySymbol = symbol
    override var symbol: BirPropertySymbol
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

    private var _field: BirFieldSymbol? = field
    override var field: BirFieldSymbol?
        get() {
            recordPropertyRead()
            return _field
        }
        set(value) {
            if (_field !== value) {
                _field = value
                invalidate()
            }
        }

    private var _getter: BirSimpleFunctionSymbol? = getter
    override var getter: BirSimpleFunctionSymbol?
        get() {
            recordPropertyRead()
            return _getter
        }
        set(value) {
            if (_getter !== value) {
                _getter = value
                invalidate()
            }
        }

    private var _setter: BirSimpleFunctionSymbol? = setter
    override var setter: BirSimpleFunctionSymbol?
        get() {
            recordPropertyRead()
            return _setter
        }
        set(value) {
            if (_setter !== value) {
                _setter = value
                invalidate()
            }
        }

    override val valueArguments: BirImplChildElementList<BirExpression?> = BirImplChildElementList(this, 1, true)


    init {
        initChild(_dispatchReceiver)
        initChild(_extensionReceiver)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _dispatchReceiver?.acceptLite(visitor)
        _extensionReceiver?.acceptLite(visitor)
        valueArguments.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._dispatchReceiver === old -> {
                this._dispatchReceiver = new as BirExpression?
            }
            this._extensionReceiver === old -> {
                this._extensionReceiver = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.valueArguments
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
