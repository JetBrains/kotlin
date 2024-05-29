/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirField
import org.jetbrains.kotlin.bir.declarations.BirPropertyWithLateBinding
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.LateBoundBirSymbol
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirPropertyWithLateBindingImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    modality: Modality,
    symbol: BirPropertySymbol,
    overriddenSymbols: List<BirPropertySymbol>,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExpect: Boolean,
    isFakeOverride: Boolean,
    backingField: BirField?,
    getter: BirSimpleFunction?,
    setter: BirSimpleFunction?,
) : BirPropertyWithLateBinding() {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        isExternal: Boolean,
        visibility: DescriptorVisibility,
        modality: Modality,
        symbol: BirPropertySymbol,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
        isDelegated: Boolean,
        isExpect: Boolean,
        isFakeOverride: Boolean,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        isExternal = isExternal,
        visibility = visibility,
        modality = modality,
        symbol = symbol,
        overriddenSymbols = emptyList(),
        isVar = isVar,
        isConst = isConst,
        isLateinit = isLateinit,
        isDelegated = isDelegated,
        isExpect = isExpect,
        isFakeOverride = isFakeOverride,
        backingField = null,
        getter = null,
        setter = null,
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

    override val symbol: BirPropertySymbol = symbol

    private var _overriddenSymbols: List<BirPropertySymbol> = overriddenSymbols
    override var overriddenSymbols: List<BirPropertySymbol>
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

    private var _isVar: Boolean = isVar
    override var isVar: Boolean
        get() {
            recordPropertyRead()
            return _isVar
        }
        set(value) {
            if (_isVar != value) {
                _isVar = value
                invalidate()
            }
        }

    private var _isConst: Boolean = isConst
    override var isConst: Boolean
        get() {
            recordPropertyRead()
            return _isConst
        }
        set(value) {
            if (_isConst != value) {
                _isConst = value
                invalidate()
            }
        }

    private var _isLateinit: Boolean = isLateinit
    override var isLateinit: Boolean
        get() {
            recordPropertyRead()
            return _isLateinit
        }
        set(value) {
            if (_isLateinit != value) {
                _isLateinit = value
                invalidate()
            }
        }

    private var _isDelegated: Boolean = isDelegated
    override var isDelegated: Boolean
        get() {
            recordPropertyRead()
            return _isDelegated
        }
        set(value) {
            if (_isDelegated != value) {
                _isDelegated = value
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

    private var _backingField: BirField? = backingField
    override var backingField: BirField?
        get() {
            recordPropertyRead()
            return _backingField
        }
        set(value) {
            if (_backingField !== value) {
                childReplaced(_backingField, value)
                _backingField = value
                invalidate()
            }
        }

    private var _getter: BirSimpleFunction? = getter
    override var getter: BirSimpleFunction?
        get() {
            recordPropertyRead()
            return _getter
        }
        set(value) {
            if (_getter !== value) {
                childReplaced(_getter, value)
                _getter = value
                invalidate()
            }
        }

    private var _setter: BirSimpleFunction? = setter
    override var setter: BirSimpleFunction?
        get() {
            recordPropertyRead()
            return _setter
        }
        set(value) {
            if (_setter !== value) {
                childReplaced(_setter, value)
                _setter = value
                invalidate()
            }
        }

    override val isBound: Boolean
        get() = _symbol != null


    init {
        initChild(_backingField)
        initChild(_getter)
        initChild(_setter)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _backingField?.acceptLite(visitor)
        _getter?.acceptLite(visitor)
        _setter?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._backingField === old -> {
                this._backingField = new as BirField?
            }
            this._getter === old -> {
                this._getter = new as BirSimpleFunction?
            }
            this._setter === old -> {
                this._setter = new as BirSimpleFunction?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    private var _symbol: BirPropertySymbol? = null

    fun acquireSymbol(symbol: LateBoundBirSymbol.PropertySymbol): BirPropertyWithLateBinding {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        return this
    }
}
