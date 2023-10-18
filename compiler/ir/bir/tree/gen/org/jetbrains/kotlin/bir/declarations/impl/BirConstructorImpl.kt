/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
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
    override val descriptor: ClassConstructorDescriptor,
    signature: IdSignature?,
    override var annotations: List<BirConstructorCall>,
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
) : BirConstructor() {
    override val owner: BirConstructorImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() = _sourceSpan
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() = _signature
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate()
            }
        }

    private var _origin: IrDeclarationOrigin = origin

    override var origin: IrDeclarationOrigin
        get() = _origin
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }

    private var _name: Name = name

    override var name: Name
        get() = _name
        set(value) {
            if (_name != value) {
                _name = value
                invalidate()
            }
        }

    private var _isExternal: Boolean = isExternal

    override var isExternal: Boolean
        get() = _isExternal
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate()
            }
        }

    private var _visibility: DescriptorVisibility = visibility

    override var visibility: DescriptorVisibility
        get() = _visibility
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate()
            }
        }

    override var typeParameters: BirChildElementList<BirTypeParameter> =
            BirChildElementList(this, 0)

    private var _isInline: Boolean = isInline

    override var isInline: Boolean
        get() = _isInline
        set(value) {
            if (_isInline != value) {
                _isInline = value
                invalidate()
            }
        }

    private var _isExpect: Boolean = isExpect

    override var isExpect: Boolean
        get() = _isExpect
        set(value) {
            if (_isExpect != value) {
                _isExpect = value
                invalidate()
            }
        }

    private var _returnType: BirType = returnType

    override var returnType: BirType
        get() = _returnType
        set(value) {
            if (_returnType != value) {
                _returnType = value
                invalidate()
            }
        }

    private var _dispatchReceiverParameter: BirValueParameter? = dispatchReceiverParameter

    override var dispatchReceiverParameter: BirValueParameter?
        get() = _dispatchReceiverParameter
        set(value) {
            if (_dispatchReceiverParameter != value) {
                replaceChild(_dispatchReceiverParameter, value)
                _dispatchReceiverParameter = value
                invalidate()
            }
        }

    private var _extensionReceiverParameter: BirValueParameter? = extensionReceiverParameter

    override var extensionReceiverParameter: BirValueParameter?
        get() = _extensionReceiverParameter
        set(value) {
            if (_extensionReceiverParameter != value) {
                replaceChild(_extensionReceiverParameter, value)
                _extensionReceiverParameter = value
                invalidate()
            }
        }

    override var valueParameters: BirChildElementList<BirValueParameter> =
            BirChildElementList(this, 1)

    private var _contextReceiverParametersCount: Int = contextReceiverParametersCount

    override var contextReceiverParametersCount: Int
        get() = _contextReceiverParametersCount
        set(value) {
            if (_contextReceiverParametersCount != value) {
                _contextReceiverParametersCount = value
                invalidate()
            }
        }

    private var _body: BirBody? = body

    override var body: BirBody?
        get() = _body
        set(value) {
            if (_body != value) {
                replaceChild(_body, value)
                _body = value
                invalidate()
            }
        }

    private var _isPrimary: Boolean = isPrimary

    override var isPrimary: Boolean
        get() = _isPrimary
        set(value) {
            if (_isPrimary != value) {
                _isPrimary = value
                invalidate()
            }
        }
    init {
        initChild(_dispatchReceiverParameter)
        initChild(_extensionReceiverParameter)
        initChild(_body)
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
        0 -> this.typeParameters
        1 -> this.valueParameters
        else -> throwChildrenListWithIdNotFound(id)
    }
}
