/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirFunctionWithLateBinding
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.symbols.LateBoundBirSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirFunctionWithLateBindingImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    isInline: Boolean,
    isExpect: Boolean,
    dispatchReceiverParameter: BirValueParameter?,
    extensionReceiverParameter: BirValueParameter?,
    contextReceiverParametersCount: Int,
    body: BirBody?,
    modality: Modality,
    symbol: BirSimpleFunctionSymbol,
    overriddenSymbols: List<BirSimpleFunctionSymbol>,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isFakeOverride: Boolean,
    isOperator: Boolean,
    isInfix: Boolean,
    correspondingPropertySymbol: BirPropertySymbol?,
) : BirFunctionWithLateBinding() {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        isExternal: Boolean,
        visibility: DescriptorVisibility,
        isInline: Boolean,
        isExpect: Boolean,
        modality: Modality,
        symbol: BirSimpleFunctionSymbol,
        isTailrec: Boolean,
        isSuspend: Boolean,
        isFakeOverride: Boolean,
        isOperator: Boolean,
        isInfix: Boolean,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        isExternal = isExternal,
        visibility = visibility,
        isInline = isInline,
        isExpect = isExpect,
        dispatchReceiverParameter = null,
        extensionReceiverParameter = null,
        contextReceiverParametersCount = 0,
        body = null,
        modality = modality,
        symbol = symbol,
        overriddenSymbols = emptyList(),
        isTailrec = isTailrec,
        isSuspend = isSuspend,
        isFakeOverride = isFakeOverride,
        isOperator = isOperator,
        isInfix = isInfix,
        correspondingPropertySymbol = null,
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

    override val signature: IdSignature? = signature

    private var _annotations: List<BirConstructorCall> = annotations
    override var annotations: List<BirConstructorCall>
        get() {
            recordPropertyRead()
            return _annotations
        }
        set(value) {
            if (_annotations != value) {
                _annotations = value
                invalidate()
            }
        }

    private var _origin: IrDeclarationOrigin = origin
    override var origin: IrDeclarationOrigin
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

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead()
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate()
            }
        }

    private var _isExternal: Boolean = isExternal
    override var isExternal: Boolean
        get() {
            recordPropertyRead()
            return _isExternal
        }
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate()
            }
        }

    private var _visibility: DescriptorVisibility = visibility
    override var visibility: DescriptorVisibility
        get() {
            recordPropertyRead()
            return _visibility
        }
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate()
            }
        }

    private var _isInline: Boolean = isInline
    override var isInline: Boolean
        get() {
            recordPropertyRead()
            return _isInline
        }
        set(value) {
            if (_isInline != value) {
                _isInline = value
                invalidate()
            }
        }

    private var _isExpect: Boolean = isExpect
    override var isExpect: Boolean
        get() {
            recordPropertyRead()
            return _isExpect
        }
        set(value) {
            if (_isExpect != value) {
                _isExpect = value
                invalidate()
            }
        }

    private var _returnType: BirType? = null
    override var returnType: BirType
        get() {
            recordPropertyRead()
            return _returnType ?: throwLateinitPropertyUninitialized("returnType")
        }
        set(value) {
            if (_returnType != value) {
                _returnType = value
                invalidate()
            }
        }

    private var _dispatchReceiverParameter: BirValueParameter? = dispatchReceiverParameter
    override var dispatchReceiverParameter: BirValueParameter?
        get() {
            recordPropertyRead()
            return _dispatchReceiverParameter
        }
        set(value) {
            if (_dispatchReceiverParameter !== value) {
                childReplaced(_dispatchReceiverParameter, value)
                _dispatchReceiverParameter = value
                invalidate()
            }
        }

    private var _extensionReceiverParameter: BirValueParameter? = extensionReceiverParameter
    override var extensionReceiverParameter: BirValueParameter?
        get() {
            recordPropertyRead()
            return _extensionReceiverParameter
        }
        set(value) {
            if (_extensionReceiverParameter !== value) {
                childReplaced(_extensionReceiverParameter, value)
                _extensionReceiverParameter = value
                invalidate()
            }
        }

    private var _contextReceiverParametersCount: Int = contextReceiverParametersCount
    override var contextReceiverParametersCount: Int
        get() {
            recordPropertyRead()
            return _contextReceiverParametersCount
        }
        set(value) {
            if (_contextReceiverParametersCount != value) {
                _contextReceiverParametersCount = value
                invalidate()
            }
        }

    private var _body: BirBody? = body
    override var body: BirBody?
        get() {
            recordPropertyRead()
            return _body
        }
        set(value) {
            if (_body !== value) {
                childReplaced(_body, value)
                _body = value
                invalidate()
            }
        }

    private var _modality: Modality = modality
    override var modality: Modality
        get() {
            recordPropertyRead()
            return _modality
        }
        set(value) {
            if (_modality != value) {
                _modality = value
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

    override val symbol: BirSimpleFunctionSymbol = symbol

    private var _overriddenSymbols: List<BirSimpleFunctionSymbol> = overriddenSymbols
    override var overriddenSymbols: List<BirSimpleFunctionSymbol>
        get() {
            recordPropertyRead()
            return _overriddenSymbols
        }
        set(value) {
            if (_overriddenSymbols != value) {
                _overriddenSymbols = value
                invalidate()
            }
        }

    private var _isTailrec: Boolean = isTailrec
    override var isTailrec: Boolean
        get() {
            recordPropertyRead()
            return _isTailrec
        }
        set(value) {
            if (_isTailrec != value) {
                _isTailrec = value
                invalidate()
            }
        }

    private var _isSuspend: Boolean = isSuspend
    override var isSuspend: Boolean
        get() {
            recordPropertyRead()
            return _isSuspend
        }
        set(value) {
            if (_isSuspend != value) {
                _isSuspend = value
                invalidate()
            }
        }

    private var _isFakeOverride: Boolean = isFakeOverride
    override var isFakeOverride: Boolean
        get() {
            recordPropertyRead()
            return _isFakeOverride
        }
        set(value) {
            if (_isFakeOverride != value) {
                _isFakeOverride = value
                invalidate()
            }
        }

    private var _isOperator: Boolean = isOperator
    override var isOperator: Boolean
        get() {
            recordPropertyRead()
            return _isOperator
        }
        set(value) {
            if (_isOperator != value) {
                _isOperator = value
                invalidate()
            }
        }

    private var _isInfix: Boolean = isInfix
    override var isInfix: Boolean
        get() {
            recordPropertyRead()
            return _isInfix
        }
        set(value) {
            if (_isInfix != value) {
                _isInfix = value
                invalidate()
            }
        }

    private var _correspondingPropertySymbol: BirPropertySymbol? = correspondingPropertySymbol
    override var correspondingPropertySymbol: BirPropertySymbol?
        get() {
            recordPropertyRead()
            return _correspondingPropertySymbol
        }
        set(value) {
            if (_correspondingPropertySymbol !== value) {
                _correspondingPropertySymbol = value
                invalidate()
            }
        }

    override val isBound: Boolean
        get() = _symbol != null

    override val typeParameters: BirImplChildElementList<BirTypeParameter> = BirImplChildElementList(this, 1, false)

    override val valueParameters: BirImplChildElementList<BirValueParameter> = BirImplChildElementList(this, 2, false)


    init {
        initChild(_dispatchReceiverParameter)
        initChild(_extensionReceiverParameter)
        initChild(_body)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        typeParameters.acceptChildrenLite(visitor)
        _dispatchReceiverParameter?.acceptLite(visitor)
        _extensionReceiverParameter?.acceptLite(visitor)
        valueParameters.acceptChildrenLite(visitor)
        _body?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._dispatchReceiverParameter === old -> {
                this._dispatchReceiverParameter = new as BirValueParameter?
            }
            this._extensionReceiverParameter === old -> {
                this._extensionReceiverParameter = new as BirValueParameter?
            }
            this._body === old -> {
                this._body = new as BirBody?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.typeParameters
            2 -> this.valueParameters
            else -> throwChildrenListWithIdNotFound(id)
        }
    }

    private var _symbol: BirSimpleFunctionSymbol? = null

    fun acquireSymbol(symbol: LateBoundBirSymbol.SimpleFunctionSymbol): BirFunctionWithLateBinding {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        return this
    }
}
