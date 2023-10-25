/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirSimpleFunctionImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor?,
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
    isFakeOverride: Boolean,
    override var overriddenSymbols: List<BirSimpleFunctionSymbol>,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isOperator: Boolean,
    isInfix: Boolean,
    correspondingPropertySymbol: BirPropertySymbol?,
) : BirSimpleFunction() {
    override val owner: BirSimpleFunctionImpl
        get() = this

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

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() {
            recordPropertyRead()
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate()
            }
        }

    override var annotations: BirChildElementList<BirConstructorCall> =
            BirChildElementList(this, 1)

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

    override var typeParameters: BirChildElementList<BirTypeParameter> =
            BirChildElementList(this, 2)

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

    private var _returnType: BirType = returnType

    override var returnType: BirType
        get() {
            recordPropertyRead()
            return _returnType
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
            if (_dispatchReceiverParameter != value) {
                replaceChild(_dispatchReceiverParameter, value)
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
            if (_extensionReceiverParameter != value) {
                replaceChild(_extensionReceiverParameter, value)
                _extensionReceiverParameter = value
                invalidate()
            }
        }

    override var valueParameters: BirChildElementList<BirValueParameter> =
            BirChildElementList(this, 3)

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
            if (_body != value) {
                replaceChild(_body, value)
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

    private var _attributeOwnerId: BirAttributeContainer = this

    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead()
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId != value) {
                _attributeOwnerId = value
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
            if (_correspondingPropertySymbol != value) {
                _correspondingPropertySymbol = value
                invalidate()
            }
        }
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

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._dispatchReceiverParameter === old -> this.dispatchReceiverParameter = new as
                BirValueParameter
            this._extensionReceiverParameter === old -> this.extensionReceiverParameter = new as
                BirValueParameter
            this._body === old -> this.body = new as BirBody
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.annotations
        2 -> this.typeParameters
        3 -> this.valueParameters
        else -> throwChildrenListWithIdNotFound(id)
    }
}
