/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirField
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.symbols.BirFieldSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirFieldImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    type: BirType,
    isFinal: Boolean,
    isStatic: Boolean,
    initializer: BirExpressionBody?,
    correspondingPropertySymbol: BirPropertySymbol?,
) : BirImplElementBase(), BirField, BirFieldSymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        isExternal: Boolean,
        visibility: DescriptorVisibility,
        type: BirType,
        isFinal: Boolean,
        isStatic: Boolean,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        isExternal = isExternal,
        visibility = visibility,
        type = type,
        isFinal = isFinal,
        isStatic = isStatic,
        initializer = null,
        correspondingPropertySymbol = null,
    )

    override val owner: BirFieldImpl
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

    override val symbol: BirFieldSymbol
        get() = this

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

    private var _isFinal: Boolean = isFinal
    override var isFinal: Boolean
        get() {
            recordPropertyRead()
            return _isFinal
        }
        set(value) {
            if (_isFinal != value) {
                _isFinal = value
                invalidate()
            }
        }

    private var _isStatic: Boolean = isStatic
    override var isStatic: Boolean
        get() {
            recordPropertyRead()
            return _isStatic
        }
        set(value) {
            if (_isStatic != value) {
                _isStatic = value
                invalidate()
            }
        }

    private var _initializer: BirExpressionBody? = initializer
    override var initializer: BirExpressionBody?
        get() {
            recordPropertyRead()
            return _initializer
        }
        set(value) {
            if (_initializer !== value) {
                childReplaced(_initializer, value)
                _initializer = value
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


    init {
        initChild(_initializer)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _initializer?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._initializer === old -> {
                this._initializer = new as BirExpressionBody?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
