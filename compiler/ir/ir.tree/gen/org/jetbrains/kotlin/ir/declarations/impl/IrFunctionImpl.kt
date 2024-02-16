/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrFunctionImpl @IrImplementationDetail constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val factory: IrFactory,
    override var name: Name,
    override var isExternal: Boolean,
    override var visibility: DescriptorVisibility,
    override val containerSource: DeserializedContainerSource?,
    override var isInline: Boolean,
    override var isExpect: Boolean,
    override var modality: Modality,
    override val symbol: IrSimpleFunctionSymbol,
    override var isTailrec: Boolean,
    override var isSuspend: Boolean,
    override var isFakeOverride: Boolean,
    override var isOperator: Boolean,
    override var isInfix: Boolean,
) : IrSimpleFunction() {
    override var annotations: List<IrConstructorCall> = emptyList()

    override lateinit var parent: IrDeclarationParent

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override var metadata: MetadataSource? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = symbol.descriptor

    override lateinit var returnType: IrType

    override var dispatchReceiverParameter: IrValueParameter? = null

    override var extensionReceiverParameter: IrValueParameter? = null

    override var valueParameters: List<IrValueParameter> = emptyList()

    override var contextReceiverParametersCount: Int = 0

    override var body: IrBody? = null

    override var attributeOwnerId: IrAttributeContainer = this

    override var originalBeforeInline: IrAttributeContainer? = null

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> = emptyList()

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    init {
        symbol.bind(this)
    }
}
