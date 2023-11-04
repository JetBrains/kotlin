/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
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
    override val descriptor: PropertyDescriptor?,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    modality: Modality,
    isFakeOverride: Boolean,
    overriddenSymbols: List<BirPropertySymbol>,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExpect: Boolean,
    backingField: BirField?,
    getter: BirSimpleFunction?,
    setter: BirSimpleFunction?,
) : BirImplElementBase(), BirProperty {
    override val owner: BirPropertyImpl
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
            recordPropertyRead(6)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(6)
            }
        }

    private var _name: Name = name

    override var name: Name
        get() {
            recordPropertyRead(7)
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate(7)
            }
        }

    private var _isExternal: Boolean = isExternal

    override var isExternal: Boolean
        get() {
            recordPropertyRead(8)
            return _isExternal
        }
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate(8)
            }
        }

    private var _visibility: DescriptorVisibility = visibility

    override var visibility: DescriptorVisibility
        get() {
            recordPropertyRead(9)
            return _visibility
        }
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate(9)
            }
        }

    private var _modality: Modality = modality

    override var modality: Modality
        get() {
            recordPropertyRead(10)
            return _modality
        }
        set(value) {
            if (_modality != value) {
                _modality = value
                invalidate(10)
            }
        }

    private var _isFakeOverride: Boolean = isFakeOverride

    override var isFakeOverride: Boolean
        get() {
            recordPropertyRead(11)
            return _isFakeOverride
        }
        set(value) {
            if (_isFakeOverride != value) {
                _isFakeOverride = value
                invalidate(11)
            }
        }

    private var _overriddenSymbols: List<BirPropertySymbol> = overriddenSymbols

    override var overriddenSymbols: List<BirPropertySymbol>
        get() {
            recordPropertyRead(12)
            return _overriddenSymbols
        }
        set(value) {
            if (_overriddenSymbols != value) {
                _overriddenSymbols = value
                invalidate(12)
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this

    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead(5)
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId != value) {
                _attributeOwnerId = value
                invalidate(5)
            }
        }

    private var _isVar: Boolean = isVar

    override var isVar: Boolean
        get() {
            recordPropertyRead(13)
            return _isVar
        }
        set(value) {
            if (_isVar != value) {
                _isVar = value
                invalidate(13)
            }
        }

    private var _isConst: Boolean = isConst

    override var isConst: Boolean
        get() {
            recordPropertyRead(14)
            return _isConst
        }
        set(value) {
            if (_isConst != value) {
                _isConst = value
                invalidate(14)
            }
        }

    private var _isLateinit: Boolean = isLateinit

    override var isLateinit: Boolean
        get() {
            recordPropertyRead(14)
            return _isLateinit
        }
        set(value) {
            if (_isLateinit != value) {
                _isLateinit = value
                invalidate(14)
            }
        }

    private var _isDelegated: Boolean = isDelegated

    override var isDelegated: Boolean
        get() {
            recordPropertyRead(14)
            return _isDelegated
        }
        set(value) {
            if (_isDelegated != value) {
                _isDelegated = value
                invalidate(14)
            }
        }

    private var _isExpect: Boolean = isExpect

    override var isExpect: Boolean
        get() {
            recordPropertyRead(14)
            return _isExpect
        }
        set(value) {
            if (_isExpect != value) {
                _isExpect = value
                invalidate(14)
            }
        }

    private var _backingField: BirField? = backingField

    override var backingField: BirField?
        get() {
            recordPropertyRead(2)
            return _backingField
        }
        set(value) {
            if (_backingField != value) {
                childReplaced(_backingField, value)
                _backingField = value
                invalidate(2)
            }
        }

    private var _getter: BirSimpleFunction? = getter

    override var getter: BirSimpleFunction?
        get() {
            recordPropertyRead(3)
            return _getter
        }
        set(value) {
            if (_getter != value) {
                childReplaced(_getter, value)
                _getter = value
                invalidate(3)
            }
        }

    private var _setter: BirSimpleFunction? = setter

    override var setter: BirSimpleFunction?
        get() {
            recordPropertyRead(4)
            return _setter
        }
        set(value) {
            if (_setter != value) {
                childReplaced(_setter, value)
                _setter = value
                invalidate(4)
            }
        }
    init {
        initChild(_backingField)
        initChild(_getter)
        initChild(_setter)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        _backingField?.acceptLite(visitor)
        _getter?.acceptLite(visitor)
        _setter?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._backingField === old -> this._backingField = new as BirField?
            this._getter === old -> this._getter = new as BirSimpleFunction?
            this._setter === old -> this._setter = new as BirSimpleFunction?
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.annotations
        else -> throwChildrenListWithIdNotFound(id)
    }
}
