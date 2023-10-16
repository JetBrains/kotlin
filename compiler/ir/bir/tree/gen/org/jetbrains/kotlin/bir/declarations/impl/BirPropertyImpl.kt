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
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class BirPropertyImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor,
    override var signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    override var origin: IrDeclarationOrigin,
    override var name: Name,
    override var isExternal: Boolean,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    override var isFakeOverride: Boolean,
    override var overriddenSymbols: List<BirPropertySymbol>,
    override var metadata: MetadataSource?,
    override var originalBeforeInline: BirAttributeContainer?,
    override val containerSource: DeserializedContainerSource?,
    override var isVar: Boolean,
    override var isConst: Boolean,
    override var isLateinit: Boolean,
    override var isDelegated: Boolean,
    override var isExpect: Boolean,
    backingField: BirField?,
    getter: BirSimpleFunction?,
    setter: BirSimpleFunction?,
) : BirProperty() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _backingField: BirField? = backingField

    override var backingField: BirField?
        get() = _backingField
        set(value) {
            if (_backingField != value) {
                replaceChild(_backingField, value)
                _backingField = value
            }
        }

    private var _getter: BirSimpleFunction? = getter

    override var getter: BirSimpleFunction?
        get() = _getter
        set(value) {
            if (_getter != value) {
                replaceChild(_getter, value)
                _getter = value
            }
        }

    private var _setter: BirSimpleFunction? = setter

    override var setter: BirSimpleFunction?
        get() = _setter
        set(value) {
            if (_setter != value) {
                replaceChild(_setter, value)
                _setter = value
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
