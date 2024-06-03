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

    override var sourceSpan: SourceSpan = sourceSpan

    override val signature: IdSignature? = signature

    override var annotations: List<BirConstructorCall> = annotations

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override var isExternal: Boolean = isExternal

    override var visibility: DescriptorVisibility = visibility

    override var isInline: Boolean = isInline

    override var isExpect: Boolean = isExpect

    private var _returnType: BirType? = null
    override var returnType: BirType
        get() {
            return _returnType ?: throwLateinitPropertyUninitialized("returnType")
        }
        set(value) {
            if (_returnType != value) {
                _returnType = value
            }
        }

    private var _dispatchReceiverParameter: BirValueParameter? = dispatchReceiverParameter
    override var dispatchReceiverParameter: BirValueParameter?
        get() {
            return _dispatchReceiverParameter
        }
        set(value) {
            if (_dispatchReceiverParameter !== value) {
                childReplaced(_dispatchReceiverParameter, value)
                _dispatchReceiverParameter = value
            }
        }

    private var _extensionReceiverParameter: BirValueParameter? = extensionReceiverParameter
    override var extensionReceiverParameter: BirValueParameter?
        get() {
            return _extensionReceiverParameter
        }
        set(value) {
            if (_extensionReceiverParameter !== value) {
                childReplaced(_extensionReceiverParameter, value)
                _extensionReceiverParameter = value
            }
        }

    override var contextReceiverParametersCount: Int = contextReceiverParametersCount

    private var _body: BirBody? = body
    override var body: BirBody?
        get() {
            return _body
        }
        set(value) {
            if (_body !== value) {
                childReplaced(_body, value)
                _body = value
            }
        }

    override var modality: Modality = modality

    override var attributeOwnerId: BirAttributeContainer = this

    override val symbol: BirSimpleFunctionSymbol = symbol

    override var overriddenSymbols: List<BirSimpleFunctionSymbol> = overriddenSymbols

    override var isTailrec: Boolean = isTailrec

    override var isSuspend: Boolean = isSuspend

    override var isFakeOverride: Boolean = isFakeOverride

    override var isOperator: Boolean = isOperator

    override var isInfix: Boolean = isInfix

    override var correspondingPropertySymbol: BirPropertySymbol? = correspondingPropertySymbol

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
