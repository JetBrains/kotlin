/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.types.impl.ReturnTypeIsNotInitializedException
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

// IMPORTANT: This class is used in the Compose IDE plugin (platform 233).
// Don't rename it or change its constructor's signature so as not to break binary compatibility when an older version of
// the Compose IDE plugin is used with a newer version of the Kotlin IDE plugin that vendors the updated compiler version.
class IrFunctionImpl @IrImplementationDetail constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrSimpleFunctionSymbol,
    override var name: Name,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    returnType: IrType,
    override var isInline: Boolean,
    override var isExternal: Boolean,
    override var isTailrec: Boolean,
    override var isSuspend: Boolean,
    override var isOperator: Boolean,
    override var isInfix: Boolean,
    override var isExpect: Boolean,
    override var isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    override var containerSource: DeserializedContainerSource? = null,
    override val factory: IrFactory = IrFactoryImpl,
) : IrSimpleFunction() {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = symbol.descriptor

    init {
        symbol.bind(this)
    }

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    override var returnType: IrType = returnType
        get() = if (field === IrUninitializedType) {
            throw ReturnTypeIsNotInitializedException(this)
        } else {
            field
        }

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override var dispatchReceiverParameter: IrValueParameter? = null
    override var extensionReceiverParameter: IrValueParameter? = null
    override var valueParameters: List<IrValueParameter> = emptyList()

    override var contextReceiverParametersCount: Int = 0

    override var body: IrBody? = null

    override var metadata: MetadataSource? = null

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> = emptyList()

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    override var correspondingPropertySymbol: IrPropertySymbol? = null
}

class IrFunctionWithLateBindingImpl @IrImplementationDetail constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override var name: Name,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    returnType: IrType,
    override var isInline: Boolean,
    override var isExternal: Boolean,
    override var isTailrec: Boolean,
    override var isSuspend: Boolean,
    override var isOperator: Boolean,
    override var isInfix: Boolean,
    override var isExpect: Boolean,
    override var isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    override val factory: IrFactory = IrFactoryImpl
) : IrFunctionWithLateBinding() {

    private var _symbol: IrSimpleFunctionSymbol? = null

    override val symbol: IrSimpleFunctionSymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    @ObsoleteDescriptorBasedAPI
    override val descriptor
        get() = _symbol?.descriptor ?: this.toIrBasedDescriptor()

    override fun acquireSymbol(symbol: IrSimpleFunctionSymbol): IrFunctionWithLateBinding {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        return this
    }

    override val isBound: Boolean
        get() = _symbol != null

    override val containerSource: DeserializedContainerSource?
        get() = null

    override lateinit var parent: IrDeclarationParent

    override var annotations: List<IrConstructorCall> = emptyList()

    override var returnType: IrType = returnType
        get() = if (field === IrUninitializedType) {
            throw ReturnTypeIsNotInitializedException(this)
        } else {
            field
        }

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override var dispatchReceiverParameter: IrValueParameter? = null
    override var extensionReceiverParameter: IrValueParameter? = null
    override var valueParameters: List<IrValueParameter> = emptyList()

    override var contextReceiverParametersCount: Int = 0

    override var body: IrBody? = null

    override var metadata: MetadataSource? = null

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> = emptyList()

    override var attributeOwnerId: IrAttributeContainer = this

    override var originalBeforeInline: IrAttributeContainer? = null

    override var correspondingPropertySymbol: IrPropertySymbol? = null
}
