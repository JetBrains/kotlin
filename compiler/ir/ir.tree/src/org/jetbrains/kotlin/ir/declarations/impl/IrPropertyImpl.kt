/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class IrPropertyCommonImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override var name: Name,
    override var visibility: DescriptorVisibility,
    override var isVar: Boolean,
    override var isConst: Boolean,
    override var isLateinit: Boolean,
    override var isDelegated: Boolean,
    override var isExternal: Boolean,
    override var isExpect: Boolean,
    override val containerSource: DeserializedContainerSource?,
) : IrProperty() {

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    override var backingField: IrField? = null

    override var getter: IrSimpleFunction? = null

    override var setter: IrSimpleFunction? = null

    override var overriddenSymbols: List<IrPropertySymbol> = emptyList()

    override var metadata: MetadataSource? = null

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null
}

class IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrPropertySymbol,
    name: Name,
    visibility: DescriptorVisibility,
    override var modality: Modality,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean,
    isExpect: Boolean = false,
    override var isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    containerSource: DeserializedContainerSource? = null,
    override val factory: IrFactory = IrFactoryImpl,
) : IrPropertyCommonImpl(
    startOffset, endOffset, origin, name, visibility, isVar, isConst, isLateinit, isDelegated, isExternal, isExpect,
    containerSource
) {
    init {
        symbol.bind(this)
    }

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor
}

class IrPropertyWithLateBindingImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    visibility: DescriptorVisibility,
    override var modality: Modality,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean,
    isExpect: Boolean,
    override var isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    override val factory: IrFactory = IrFactoryImpl,
) : IrPropertyCommonImpl(
    startOffset, endOffset, origin, name, visibility, isVar, isConst, isLateinit, isDelegated, isExternal, isExpect,
    containerSource = null,
), IrPropertyWithLateBinding {
    private var _symbol: IrPropertySymbol? = null

    override val symbol: IrPropertySymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    @ObsoleteDescriptorBasedAPI
    override val descriptor
        get() = _symbol?.descriptor ?: this.toIrBasedDescriptor()

    override fun acquireSymbol(symbol: IrPropertySymbol): IrProperty {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        return this
    }

    override val isBound: Boolean
        get() = _symbol != null
}
