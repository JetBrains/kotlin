/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
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
    override val descriptor: ParameterDescriptor,
    signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    type: BirType,
    override val isAssignable: Boolean,
    index: Int,
    varargElementType: BirType?,
    isCrossinline: Boolean,
    isNoinline: Boolean,
    isHidden: Boolean,
    defaultValue: BirExpressionBody?,
) : BirValueParameter() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() = _sourceSpan
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _signature: IdSignature = signature

    override var signature: IdSignature
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

    private var _type: BirType = type

    override var type: BirType
        get() = _type
        set(value) {
            if (_type != value) {
                _type = value
                invalidate()
            }
        }

    private var _index: Int = index

    override var index: Int
        get() = _index
        set(value) {
            if (_index != value) {
                _index = value
                invalidate()
            }
        }

    private var _varargElementType: BirType? = varargElementType

    override var varargElementType: BirType?
        get() = _varargElementType
        set(value) {
            if (_varargElementType != value) {
                _varargElementType = value
                invalidate()
            }
        }

    private var _isCrossinline: Boolean = isCrossinline

    override var isCrossinline: Boolean
        get() = _isCrossinline
        set(value) {
            if (_isCrossinline != value) {
                _isCrossinline = value
                invalidate()
            }
        }

    private var _isNoinline: Boolean = isNoinline

    override var isNoinline: Boolean
        get() = _isNoinline
        set(value) {
            if (_isNoinline != value) {
                _isNoinline = value
                invalidate()
            }
        }

    private var _isHidden: Boolean = isHidden

    override var isHidden: Boolean
        get() = _isHidden
        set(value) {
            if (_isHidden != value) {
                _isHidden = value
                invalidate()
            }
        }

    private var _defaultValue: BirExpressionBody? = defaultValue

    override var defaultValue: BirExpressionBody?
        get() = _defaultValue
        set(value) {
            if (_defaultValue != value) {
                replaceChild(_defaultValue, value)
                _defaultValue = value
                invalidate()
            }
        }
    init {
        initChild(_defaultValue)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._defaultValue === old -> this.defaultValue = new as BirExpressionBody
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
