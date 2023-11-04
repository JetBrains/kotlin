/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.BirImplChildElementList
import org.jetbrains.kotlin.bir.BirImplElementBase
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirConstructor
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirConstructorImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: ClassConstructorDescriptor?,
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
    isPrimary: Boolean,
) : BirImplElementBase(), BirConstructor {
    override val owner: BirConstructorImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan

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

    override val annotations: BirChildElementList<BirConstructorCall> =
            BirImplChildElementList(this, 1, false)

    private var _origin: IrDeclarationOrigin = origin

    override var origin: IrDeclarationOrigin
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

    private var _name: Name = name

    override var name: Name
        get() {
            recordPropertyRead(8)
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate(8)
            }
        }

    private var _isExternal: Boolean = isExternal

    override var isExternal: Boolean
        get() {
            recordPropertyRead(9)
            return _isExternal
        }
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate(9)
            }
        }

    private var _visibility: DescriptorVisibility = visibility

    override var visibility: DescriptorVisibility
        get() {
            recordPropertyRead(10)
            return _visibility
        }
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate(10)
            }
        }

    override val typeParameters: BirChildElementList<BirTypeParameter> =
            BirImplChildElementList(this, 2, false)

    private var _isInline: Boolean = isInline

    override var isInline: Boolean
        get() {
            recordPropertyRead(11)
            return _isInline
        }
        set(value) {
            if (_isInline != value) {
                _isInline = value
                invalidate(11)
            }
        }

    private var _isExpect: Boolean = isExpect

    override var isExpect: Boolean
        get() {
            recordPropertyRead(12)
            return _isExpect
        }
        set(value) {
            if (_isExpect != value) {
                _isExpect = value
                invalidate(12)
            }
        }

    private var _returnType: BirType = returnType

    override var returnType: BirType
        get() {
            recordPropertyRead(13)
            return _returnType
        }
        set(value) {
            if (_returnType != value) {
                _returnType = value
                invalidate(13)
            }
        }

    private var _dispatchReceiverParameter: BirValueParameter? = dispatchReceiverParameter

    override var dispatchReceiverParameter: BirValueParameter?
        get() {
            recordPropertyRead(4)
            return _dispatchReceiverParameter
        }
        set(value) {
            if (_dispatchReceiverParameter != value) {
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
            if (_extensionReceiverParameter != value) {
                childReplaced(_extensionReceiverParameter, value)
                _extensionReceiverParameter = value
                invalidate(5)
            }
        }

    override val valueParameters: BirChildElementList<BirValueParameter> =
            BirImplChildElementList(this, 3, false)

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
            if (_body != value) {
                childReplaced(_body, value)
                _body = value
                invalidate(6)
            }
        }

    private var _isPrimary: Boolean = isPrimary

    override var isPrimary: Boolean
        get() {
            recordPropertyRead(14)
            return _isPrimary
        }
        set(value) {
            if (_isPrimary != value) {
                _isPrimary = value
                invalidate(14)
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
            this._dispatchReceiverParameter === old -> this._dispatchReceiverParameter = new as
                BirValueParameter?
            this._extensionReceiverParameter === old -> this._extensionReceiverParameter = new as
                BirValueParameter?
            this._body === old -> this._body = new as BirBody?
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
