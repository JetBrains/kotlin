/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirSimpleFunctionImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    isInline: Boolean,
    isExpect: Boolean,
    returnType: BirType,
    dispatchReceiverParameter: BirValueParameter?,
    extensionReceiverParameter: BirValueParameter?,
    contextReceiverParametersCount: Int,
    body: BirBody?,
    modality: Modality,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isFakeOverride: Boolean,
    isOperator: Boolean,
    isInfix: Boolean,
    correspondingPropertySymbol: BirPropertySymbol?,
    overriddenSymbols: List<BirSimpleFunctionSymbol>,
) : BirImplElementBase(BirSimpleFunction), BirSimpleFunction {
    override val owner: BirSimpleFunctionImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(14)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(14)
            }
        }

    private var _signature: IdSignature? = signature
    override var signature: IdSignature?
        get() {
            recordPropertyRead(14)
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate(14)
            }
        }

    private var _origin: IrDeclarationOrigin = origin
    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead(8)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(8)
            }
        }

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead(9)
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate(9)
            }
        }

    private var _isExternal: Boolean = isExternal
    override var isExternal: Boolean
        get() {
            recordPropertyRead(10)
            return _isExternal
        }
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate(10)
            }
        }

    private var _visibility: DescriptorVisibility = visibility
    override var visibility: DescriptorVisibility
        get() {
            recordPropertyRead(11)
            return _visibility
        }
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate(11)
            }
        }

    private var _isInline: Boolean = isInline
    override var isInline: Boolean
        get() {
            recordPropertyRead(12)
            return _isInline
        }
        set(value) {
            if (_isInline != value) {
                _isInline = value
                invalidate(12)
            }
        }

    private var _isExpect: Boolean = isExpect
    override var isExpect: Boolean
        get() {
            recordPropertyRead(13)
            return _isExpect
        }
        set(value) {
            if (_isExpect != value) {
                _isExpect = value
                invalidate(13)
            }
        }

    private var _returnType: BirType = returnType
    override var returnType: BirType
        get() {
            recordPropertyRead(14)
            return _returnType
        }
        set(value) {
            if (_returnType != value) {
                _returnType = value
                invalidate(14)
            }
        }

    private var _dispatchReceiverParameter: BirValueParameter? = dispatchReceiverParameter
    override var dispatchReceiverParameter: BirValueParameter?
        get() {
            recordPropertyRead(4)
            return _dispatchReceiverParameter
        }
        set(value) {
            if (_dispatchReceiverParameter !== value) {
                childReplaced(_dispatchReceiverParameter, value)
                _dispatchReceiverParameter = value
                invalidate(4)
            }
        }

    private var _extensionReceiverParameter: BirValueParameter? = extensionReceiverParameter
    override var extensionReceiverParameter: BirValueParameter?
        get() {
            recordPropertyRead(5)
            return _extensionReceiverParameter
        }
        set(value) {
            if (_extensionReceiverParameter !== value) {
                childReplaced(_extensionReceiverParameter, value)
                _extensionReceiverParameter = value
                invalidate(5)
            }
        }

    private var _contextReceiverParametersCount: Int = contextReceiverParametersCount
    override var contextReceiverParametersCount: Int
        get() {
            recordPropertyRead(14)
            return _contextReceiverParametersCount
        }
        set(value) {
            if (_contextReceiverParametersCount != value) {
                _contextReceiverParametersCount = value
                invalidate(14)
            }
        }

    private var _body: BirBody? = body
    override var body: BirBody?
        get() {
            recordPropertyRead(6)
            return _body
        }
        set(value) {
            if (_body !== value) {
                childReplaced(_body, value)
                _body = value
                invalidate(6)
            }
        }

    private var _modality: Modality = modality
    override var modality: Modality
        get() {
            recordPropertyRead(14)
            return _modality
        }
        set(value) {
            if (_modality != value) {
                _modality = value
                invalidate(14)
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this
    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead(7)
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId !== value) {
                _attributeOwnerId = value
                invalidate(7)
            }
        }

    private var _isTailrec: Boolean = isTailrec
    override var isTailrec: Boolean
        get() {
            recordPropertyRead(14)
            return _isTailrec
        }
        set(value) {
            if (_isTailrec != value) {
                _isTailrec = value
                invalidate(14)
            }
        }

    private var _isSuspend: Boolean = isSuspend
    override var isSuspend: Boolean
        get() {
            recordPropertyRead(14)
            return _isSuspend
        }
        set(value) {
            if (_isSuspend != value) {
                _isSuspend = value
                invalidate(14)
            }
        }

    private var _isFakeOverride: Boolean = isFakeOverride
    override var isFakeOverride: Boolean
        get() {
            recordPropertyRead(14)
            return _isFakeOverride
        }
        set(value) {
            if (_isFakeOverride != value) {
                _isFakeOverride = value
                invalidate(14)
            }
        }

    private var _isOperator: Boolean = isOperator
    override var isOperator: Boolean
        get() {
            recordPropertyRead(14)
            return _isOperator
        }
        set(value) {
            if (_isOperator != value) {
                _isOperator = value
                invalidate(14)
            }
        }

    private var _isInfix: Boolean = isInfix
    override var isInfix: Boolean
        get() {
            recordPropertyRead(14)
            return _isInfix
        }
        set(value) {
            if (_isInfix != value) {
                _isInfix = value
                invalidate(14)
            }
        }

    private var _correspondingPropertySymbol: BirPropertySymbol? = correspondingPropertySymbol
    override var correspondingPropertySymbol: BirPropertySymbol?
        get() {
            recordPropertyRead(14)
            return _correspondingPropertySymbol
        }
        set(value) {
            if (_correspondingPropertySymbol != value) {
                _correspondingPropertySymbol = value
                invalidate(14)
            }
        }

    private var _overriddenSymbols: List<BirSimpleFunctionSymbol> = overriddenSymbols
    override var overriddenSymbols: List<BirSimpleFunctionSymbol>
        get() {
            recordPropertyRead(14)
            return _overriddenSymbols
        }
        set(value) {
            if (_overriddenSymbols != value) {
                _overriddenSymbols = value
                invalidate(14)
            }
        }

    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 1, false)
    override val typeParameters: BirImplChildElementList<BirTypeParameter> = BirImplChildElementList(this, 2, false)
    override val valueParameters: BirImplChildElementList<BirValueParameter> = BirImplChildElementList(this, 3, false)

    init {
        initChild(_dispatchReceiverParameter)
        initChild(_extensionReceiverParameter)
        initChild(_body)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        typeParameters.acceptChildrenLite(visitor)
        _dispatchReceiverParameter?.acceptLite(visitor)
        _extensionReceiverParameter?.acceptLite(visitor)
        valueParameters.acceptChildrenLite(visitor)
        _body?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
        return when {
            this._dispatchReceiverParameter === old -> {
                this._dispatchReceiverParameter = new as BirValueParameter?
                4
            }
            this._extensionReceiverParameter === old -> {
                this._extensionReceiverParameter = new as BirValueParameter?
                5
            }
            this._body === old -> {
                this._body = new as BirBody?
                6
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.annotations
            2 -> this.typeParameters
            3 -> this.valueParameters
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
