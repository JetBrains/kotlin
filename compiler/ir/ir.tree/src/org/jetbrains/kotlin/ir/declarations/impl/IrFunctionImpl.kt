/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.carriers.FunctionCarrier
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

abstract class IrFunctionCommonImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    visibility: Visibility,
    override val modality: Modality,
    returnType: IrType,
    isInline: Boolean,
    isExternal: Boolean,
    override val isTailrec: Boolean,
    override val isSuspend: Boolean,
    override val isOperator: Boolean,
    isExpect: Boolean,
    override val isFakeOverride: Boolean
) :
    IrFunctionBase<FunctionCarrier>(startOffset, endOffset, origin, name, visibility, isInline, isExternal, isExpect, returnType),
    IrSimpleFunction,
    FunctionCarrier {

    @DescriptorBasedIr
    abstract override val descriptor: FunctionDescriptor

    override var overriddenSymbolsField: List<IrSimpleFunctionSymbol> = emptyList()

    override var overriddenSymbols: List<IrSimpleFunctionSymbol>
        get() = getCarrier().overriddenSymbolsField
        set(v) {
            if (overriddenSymbols !== v) {
                setCarrier().overriddenSymbolsField = v
            }
        }

    override var attributeOwnerIdField: IrAttributeContainer = this

    override var attributeOwnerId: IrAttributeContainer
        get() = getCarrier().attributeOwnerIdField
        set(v) {
            if (attributeOwnerId !== v) {
                setCarrier().attributeOwnerIdField = v
            }
        }

    override var correspondingPropertySymbolField: IrPropertySymbol? = null

    override var correspondingPropertySymbol: IrPropertySymbol?
        get() = getCarrier().correspondingPropertySymbolField
        set(v) {
            if (correspondingPropertySymbol !== v) {
                setCarrier().correspondingPropertySymbolField = v
            }
        }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)
}

class IrFunctionImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrSimpleFunctionSymbol,
    name: Name,
    visibility: Visibility,
    override val modality: Modality,
    returnType: IrType,
    isInline: Boolean,
    isExternal: Boolean,
    override val isTailrec: Boolean,
    override val isSuspend: Boolean,
    override val isOperator: Boolean,
    isExpect: Boolean,
    override val isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE
) : IrFunctionCommonImpl(startOffset, endOffset, origin, name, visibility, modality, returnType, isInline,
    isExternal, isTailrec, isSuspend, isOperator, isExpect, isFakeOverride) {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrSimpleFunctionSymbol,
        returnType: IrType,
        descriptor: FunctionDescriptor,
        name: Name = descriptor.name
    ) : this(
        startOffset, endOffset, origin, symbol,
        name = name,
        visibility = descriptor.visibility,
        modality = descriptor.modality,
        returnType = returnType,
        isInline = descriptor.isInline,
        isExternal = descriptor.isExternal,
        isTailrec = descriptor.isTailrec,
        isSuspend = descriptor.isSuspend,
        isOperator = descriptor.isOperator,
        isExpect = descriptor.isExpect
    )

    // Used by kotlin-native in InteropLowering.kt and IrUtils2.kt
    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: FunctionDescriptor,
        returnType: IrType
    ) : this(
        startOffset, endOffset, origin,
        IrSimpleFunctionSymbolImpl(descriptor), returnType, descriptor
    )

    @DescriptorBasedIr
    override val descriptor: FunctionDescriptor get() = symbol.descriptor

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)
}

class IrFakeOverrideFunctionImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    override var visibility: Visibility,
    override var modality: Modality,
    returnType: IrType,
    isInline: Boolean,
    isExternal: Boolean,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isOperator: Boolean,
    isExpect: Boolean
) : IrFunctionCommonImpl(startOffset, endOffset, origin, name, visibility, modality, returnType, isInline,
    isExternal, isTailrec, isSuspend, isOperator, isExpect,
    isFakeOverride = true)
{
    private var _symbol: IrSimpleFunctionSymbol? = null

    override val symbol: IrSimpleFunctionSymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    @DescriptorBasedIr
    override val descriptor
        get() = _symbol?.descriptor ?: WrappedSimpleFunctionDescriptor()

    @OptIn(DescriptorBasedIr::class)
    fun acquireSymbol(symbol: IrSimpleFunctionSymbol) {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        (symbol.descriptor as? WrappedSimpleFunctionDescriptor)?.bind(this)
    }
}
