/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirPropertyImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    modality: Modality,
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
) : BirImplElementBase(), BirProperty, BirPropertySymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        isExternal: Boolean,
        visibility: DescriptorVisibility,
        modality: Modality,
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

    override val owner: BirPropertyImpl
        get() = this

    override val isBound: Boolean
        get() = true

    override var sourceSpan: SourceSpan = sourceSpan

    override val signature: IdSignature? = signature

    override var annotations: List<BirConstructorCall> = annotations

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override var isExternal: Boolean = isExternal

    override var visibility: DescriptorVisibility = visibility

    override var modality: Modality = modality

    override var attributeOwnerId: BirAttributeContainer = this

    override val symbol: BirPropertySymbol
        get() = this

    override var overriddenSymbols: List<BirPropertySymbol> = overriddenSymbols

    override var isVar: Boolean = isVar

    override var isConst: Boolean = isConst

    override var isLateinit: Boolean = isLateinit

    override var isDelegated: Boolean = isDelegated

    override var isExpect: Boolean = isExpect

    override var isFakeOverride: Boolean = isFakeOverride

    private var _backingField: BirField? = backingField
    override var backingField: BirField?
        get() {
            return _backingField
        }
        set(value) {
            if (_backingField !== value) {
                childReplaced(_backingField, value)
                _backingField = value
            }
        }

    private var _getter: BirSimpleFunction? = getter
    override var getter: BirSimpleFunction?
        get() {
            return _getter
        }
        set(value) {
            if (_getter !== value) {
                childReplaced(_getter, value)
                _getter = value
            }
        }

    private var _setter: BirSimpleFunction? = setter
    override var setter: BirSimpleFunction?
        get() {
            return _setter
        }
        set(value) {
            if (_setter !== value) {
                childReplaced(_setter, value)
                _setter = value
            }
        }


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
}
