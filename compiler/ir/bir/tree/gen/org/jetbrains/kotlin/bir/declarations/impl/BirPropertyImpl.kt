/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirField
import org.jetbrains.kotlin.bir.declarations.BirProperty
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirPropertyImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor,
    signature: IdSignature?,
    override var annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    modality: Modality,
    isFakeOverride: Boolean,
    override var overriddenSymbols: List<BirPropertySymbol>,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExpect: Boolean,
    backingField: BirField?,
    getter: BirSimpleFunction?,
    setter: BirSimpleFunction?,
) : BirProperty() {
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

    private var _modality: Modality = modality

    override var modality: Modality
        get() = _modality
        set(value) {
            if (_modality != value) {
                _modality = value
                invalidate()
            }
        }

    private var _isFakeOverride: Boolean = isFakeOverride

    override var isFakeOverride: Boolean
        get() = _isFakeOverride
        set(value) {
            if (_isFakeOverride != value) {
                _isFakeOverride = value
                invalidate()
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this

    override var attributeOwnerId: BirAttributeContainer
        get() = _attributeOwnerId
        set(value) {
            if (_attributeOwnerId != value) {
                _attributeOwnerId = value
                invalidate()
            }
        }

    private var _isVar: Boolean = isVar

    override var isVar: Boolean
        get() = _isVar
        set(value) {
            if (_isVar != value) {
                _isVar = value
                invalidate()
            }
        }

    private var _isConst: Boolean = isConst

    override var isConst: Boolean
        get() = _isConst
        set(value) {
            if (_isConst != value) {
                _isConst = value
                invalidate()
            }
        }

    private var _isLateinit: Boolean = isLateinit

    override var isLateinit: Boolean
        get() = _isLateinit
        set(value) {
            if (_isLateinit != value) {
                _isLateinit = value
                invalidate()
            }
        }

    private var _isDelegated: Boolean = isDelegated

    override var isDelegated: Boolean
        get() = _isDelegated
        set(value) {
            if (_isDelegated != value) {
                _isDelegated = value
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

    private var _backingField: BirField? = backingField

    override var backingField: BirField?
        get() = _backingField
        set(value) {
            if (_backingField != value) {
                replaceChild(_backingField, value)
                _backingField = value
                invalidate()
            }
        }

    private var _getter: BirSimpleFunction? = getter

    override var getter: BirSimpleFunction?
        get() = _getter
        set(value) {
            if (_getter != value) {
                replaceChild(_getter, value)
                _getter = value
                invalidate()
            }
        }

    private var _setter: BirSimpleFunction? = setter

    override var setter: BirSimpleFunction?
        get() = _setter
        set(value) {
            if (_setter != value) {
                replaceChild(_setter, value)
                _setter = value
                invalidate()
            }
        }
    init {
        initChild(_backingField)
        initChild(_getter)
        initChild(_setter)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._backingField === old -> this.backingField = new as BirField
            this._getter === old -> this.getter = new as BirSimpleFunction
            this._setter === old -> this.setter = new as BirSimpleFunction
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
