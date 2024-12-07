/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrFunctionWithLateBindingImpl @IrImplementationDetail constructor(
    override var startOffset: Int,
    override var endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val factory: IrFactory,
    override var name: Name,
    override var isExternal: Boolean,
    override var visibility: DescriptorVisibility,
    override var isInline: Boolean,
    override var isExpect: Boolean,
    override var modality: Modality,
    override var isFakeOverride: Boolean,
    override var isTailrec: Boolean,
    override var isSuspend: Boolean,
    override var isOperator: Boolean,
    override var isInfix: Boolean,
) : IrFunctionWithLateBinding() {
    override var annotations: List<IrConstructorCall> = emptyList()

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override val containerSource: DeserializedContainerSource?
        get() = null

    override var metadata: MetadataSource? = null

    override lateinit var returnType: IrType

    override var body: IrBody? = null

    override var attributeOwnerId: IrAttributeContainer = this

    override var originalBeforeInline: IrAttributeContainer? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = _symbol?.descriptor ?: this.toIrBasedDescriptor()

    override val symbol: IrSimpleFunctionSymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> = emptyList()

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    override val isBound: Boolean
        get() = _symbol != null

    private var _symbol: IrSimpleFunctionSymbol? = null

    override fun acquireSymbol(symbol: IrSimpleFunctionSymbol): IrFunctionWithLateBinding {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        return this
    }
}
