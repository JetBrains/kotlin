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
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirFunctionReference
import org.jetbrains.kotlin.bir.symbols.BirFunctionSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirFunctionReferenceImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    symbol: BirFunctionSymbol,
    dispatchReceiver: BirExpression?,
    extensionReceiver: BirExpression?,
    origin: IrStatementOrigin?,
    typeArguments: List<BirType?>,
    reflectionTarget: BirFunctionSymbol?,
) : BirFunctionReference() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(10)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(10)
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this

    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead(4)
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId != value) {
                _attributeOwnerId = value
                invalidate(4)
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

    private var _symbol: BirFunctionSymbol = symbol

    override var symbol: BirFunctionSymbol
        get() {
            recordPropertyRead(6)
            return _symbol
        }
        set(value) {
            if (_symbol != value) {
                _symbol = value
                invalidate(6)
            }
        }

    private var _dispatchReceiver: BirExpression? = dispatchReceiver

    override var dispatchReceiver: BirExpression?
        get() {
            recordPropertyRead(2)
            return _dispatchReceiver
        }
        set(value) {
            if (_dispatchReceiver != value) {
                childReplaced(_dispatchReceiver, value)
                _dispatchReceiver = value
                invalidate(2)
            }
        }

    private var _extensionReceiver: BirExpression? = extensionReceiver

    override var extensionReceiver: BirExpression?
        get() {
            recordPropertyRead(3)
            return _extensionReceiver
        }
        set(value) {
            if (_extensionReceiver != value) {
                childReplaced(_extensionReceiver, value)
                _extensionReceiver = value
                invalidate(3)
            }
        }

    private var _origin: IrStatementOrigin? = origin

    override var origin: IrStatementOrigin?
        get() {
            recordPropertyRead(7)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(7)
            }
        }

    override val valueArguments: BirImplChildElementList<BirExpression?> =
            BirImplChildElementList(this, 1, true)

    private var _typeArguments: List<BirType?> = typeArguments

    override var typeArguments: List<BirType?>
        get() {
            recordPropertyRead(8)
            return _typeArguments
        }
        set(value) {
            if (_typeArguments != value) {
                _typeArguments = value
                invalidate(8)
            }
        }

    private var _reflectionTarget: BirFunctionSymbol? = reflectionTarget

    override var reflectionTarget: BirFunctionSymbol?
        get() {
            recordPropertyRead(9)
            return _reflectionTarget
        }
        set(value) {
            if (_reflectionTarget != value) {
                _reflectionTarget = value
                invalidate(9)
            }
        }
    init {
        initChild(_dispatchReceiver)
        initChild(_extensionReceiver)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _dispatchReceiver?.acceptLite(visitor)
        _extensionReceiver?.acceptLite(visitor)
        valueArguments.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int = when {
        this._dispatchReceiver === old -> {
            this._dispatchReceiver = new as BirExpression?
            2
        }
        this._extensionReceiver === old -> {
            this._extensionReceiver = new as BirExpression?
            3
        }
        else -> throwChildForReplacementNotFound(old)
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.valueArguments
        else -> throwChildrenListWithIdNotFound(id)
    }
}
