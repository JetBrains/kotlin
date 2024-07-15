/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrPropertyWithLateBinding
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrPropertyWithLateBindingImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    modality: Modality,
    isFakeOverride: Boolean,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExpect: Boolean,
) : IrPropertyWithLateBinding(
    startOffset = startOffset,
    endOffset = endOffset,
    origin = origin,
    factory = factory,
    name = name,
    isExternal = isExternal,
    visibility = visibility,
    modality = modality,
    isFakeOverride = isFakeOverride,
    isVar = isVar,
    isConst = isConst,
    isLateinit = isLateinit,
    isDelegated = isDelegated,
    isExpect = isExpect,
) {
    override val containerSource: DeserializedContainerSource?
        get() = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = _symbol?.descriptor ?: this.toIrBasedDescriptor()

    override val symbol: IrPropertySymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

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
