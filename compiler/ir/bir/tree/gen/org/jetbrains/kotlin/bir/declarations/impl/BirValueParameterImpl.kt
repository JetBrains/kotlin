/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.symbols.BirValueParameterSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirValueParameterImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
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
) : BirImplElementBase(), BirValueParameter, BirValueParameterSymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        type: BirType,
        isAssignable: Boolean,
        index: Int,
        varargElementType: BirType?,
        isCrossinline: Boolean,
        isNoinline: Boolean,
        isHidden: Boolean,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        type = type,
        isAssignable = isAssignable,
        index = index,
        varargElementType = varargElementType,
        isCrossinline = isCrossinline,
        isNoinline = isNoinline,
        isHidden = isHidden,
        defaultValue = null,
    )

    override val owner: BirValueParameterImpl
        get() = this

    override val isBound: Boolean
        get() = true

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

    override val isAssignable: Boolean = isAssignable

    override val symbol: BirValueParameterSymbol
        get() = this

    private var _index: Int = index
    override var index: Int
        get() {
            recordPropertyRead()
            return _index
        }
        set(value) {
            if (_index != value) {
                _index = value
                invalidate()
            }
        }

    private var _varargElementType: BirType? = varargElementType
    override var varargElementType: BirType?
        get() {
            recordPropertyRead()
            return _varargElementType
        }
        set(value) {
            if (_varargElementType != value) {
                _varargElementType = value
                invalidate()
            }
        }

    private var _isCrossinline: Boolean = isCrossinline
    override var isCrossinline: Boolean
        get() {
            recordPropertyRead()
            return _isCrossinline
        }
        set(value) {
            if (_isCrossinline != value) {
                _isCrossinline = value
                invalidate()
            }
        }

    private var _isNoinline: Boolean = isNoinline
    override var isNoinline: Boolean
        get() {
            recordPropertyRead()
            return _isNoinline
        }
        set(value) {
            if (_isNoinline != value) {
                _isNoinline = value
                invalidate()
            }
        }

    private var _isHidden: Boolean = isHidden
    override var isHidden: Boolean
        get() {
            recordPropertyRead()
            return _isHidden
        }
        set(value) {
            if (_isHidden != value) {
                _isHidden = value
                invalidate()
            }
        }

    private var _defaultValue: BirExpressionBody? = defaultValue
    override var defaultValue: BirExpressionBody?
        get() {
            recordPropertyRead()
            return _defaultValue
        }
        set(value) {
            if (_defaultValue !== value) {
                childReplaced(_defaultValue, value)
                _defaultValue = value
                invalidate()
            }
        }


    init {
        initChild(_defaultValue)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _defaultValue?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._defaultValue === old -> {
                this._defaultValue = new as BirExpressionBody?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
