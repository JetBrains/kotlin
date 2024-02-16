/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrPropertyWithLateBindingImpl @IrImplementationDetail constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val factory: IrFactory,
    override var name: Name,
    override var isExternal: Boolean,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    override var isVar: Boolean,
    override var isConst: Boolean,
    override var isLateinit: Boolean,
    override var isDelegated: Boolean,
    override var isExpect: Boolean,
    override var isFakeOverride: Boolean,
) : IrPropertyWithLateBinding() {
    override var annotations: List<IrConstructorCall> = emptyList()

    override lateinit var parent: IrDeclarationParent

    override var metadata: MetadataSource? = null

    override var attributeOwnerId: IrAttributeContainer = this

    override var originalBeforeInline: IrAttributeContainer? = null

    override val containerSource: DeserializedContainerSource?
        get() = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor

    override val symbol: IrPropertySymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    override var overriddenSymbols: List<IrPropertySymbol> = emptyList()

    override var backingField: IrField? = null

    override var getter: IrSimpleFunction? = null

    override var setter: IrSimpleFunction? = null

    override val isBound: Boolean
        get() = _symbol != null

    private var _symbol: IrPropertySymbol? = null

    override fun acquireSymbol(symbol: IrPropertySymbol): IrPropertyWithLateBinding {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        return this
    }
}
