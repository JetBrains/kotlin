/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirValueParameterImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: ParameterDescriptor?,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    type: BirType,
    isAssignable: Boolean,
    index: Int,
    varargElementType: BirType?,
    isCrossinline: Boolean,
    isNoinline: Boolean,
    isHidden: Boolean,
    defaultValue: BirExpressionBody?,
) : BirImplElementBase(), BirValueParameter {
    override val owner: BirValueParameterImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(12)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(12)
            }
        }

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() {
            recordPropertyRead(13)
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate(13)
            }
        }

    override val annotations: BirChildElementList<BirConstructorCall> =
            BirImplChildElementList(this, 1, false)

    private var _origin: IrDeclarationOrigin = origin

    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead(3)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(3)
            }
        }

    private var _name: Name = name

    override var name: Name
        get() {
            recordPropertyRead(4)
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
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

    private var _isAssignable: Boolean = isAssignable

    override var isAssignable: Boolean
        get() {
            recordPropertyRead(6)
            return _isAssignable
        }
        set(value) {
            if (_isAssignable != value) {
                _isAssignable = value
                invalidate(6)
            }
        }

    private var _index: Int = index

    override var index: Int
        get() {
            recordPropertyRead(7)
            return _index
        }
        set(value) {
            if (_index != value) {
                _index = value
                invalidate(7)
            }
        }

    private var _varargElementType: BirType? = varargElementType

    override var varargElementType: BirType?
        get() {
            recordPropertyRead(8)
            return _varargElementType
        }
        set(value) {
            if (_varargElementType != value) {
                _varargElementType = value
                invalidate(8)
            }
        }

    private var _isCrossinline: Boolean = isCrossinline

    override var isCrossinline: Boolean
        get() {
            recordPropertyRead(9)
            return _isCrossinline
        }
        set(value) {
            if (_isCrossinline != value) {
                _isCrossinline = value
                invalidate(9)
            }
        }

    private var _isNoinline: Boolean = isNoinline

    override var isNoinline: Boolean
        get() {
            recordPropertyRead(10)
            return _isNoinline
        }
        set(value) {
            if (_isNoinline != value) {
                _isNoinline = value
                invalidate(10)
            }
        }

    private var _isHidden: Boolean = isHidden

    override var isHidden: Boolean
        get() {
            recordPropertyRead(11)
            return _isHidden
        }
        set(value) {
            if (_isHidden != value) {
                _isHidden = value
                invalidate(11)
            }
        }

    private var _defaultValue: BirExpressionBody? = defaultValue

    override var defaultValue: BirExpressionBody?
        get() {
            recordPropertyRead(2)
            return _defaultValue
        }
        set(value) {
            if (_defaultValue != value) {
                childReplaced(_defaultValue, value)
                _defaultValue = value
                invalidate(2)
            }
        }
    init {
        initChild(_defaultValue)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        _defaultValue?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._defaultValue === old -> this._defaultValue = new as BirExpressionBody?
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.annotations
        else -> throwChildrenListWithIdNotFound(id)
    }
}
