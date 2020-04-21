/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.carriers.FunctionCarrier
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class IrFunctionImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrSimpleFunctionSymbol,
    name: Name = symbol.descriptor.name,
    visibility: Visibility = symbol.descriptor.visibility,
    override val modality: Modality = symbol.descriptor.modality,
    returnType: IrType,
    isInline: Boolean = symbol.descriptor.isInline,
    isExternal: Boolean = symbol.descriptor.isExternal,
    override val isTailrec: Boolean = symbol.descriptor.isTailrec,
    override val isSuspend: Boolean = symbol.descriptor.isSuspend,
    override val isOperator: Boolean = symbol.descriptor.isOperator,
    isExpect: Boolean = symbol.descriptor.isExpect,
    override val isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE
) :
    IrFunctionBase<FunctionCarrier>(startOffset, endOffset, origin, name, visibility, isInline, isExternal, isExpect, returnType),
    IrSimpleFunction,
    FunctionCarrier {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrSimpleFunctionSymbol,
        returnType: IrType,
        visibility: Visibility = symbol.descriptor.visibility,
        modality: Modality = symbol.descriptor.modality
    ) : this(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        visibility,
        modality,
        returnType,
        isInline = symbol.descriptor.isInline,
        isExternal = symbol.descriptor.isExternal,
        isTailrec = symbol.descriptor.isTailrec,
        isSuspend = symbol.descriptor.isSuspend,
        isExpect = symbol.descriptor.isExpect,
        isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
        isOperator = symbol.descriptor.isOperator
    )

    override val descriptor: FunctionDescriptor = symbol.descriptor

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

    // Used by kotlin-native in InteropLowering.kt and IrUtils2.kt
    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: FunctionDescriptor,
        returnType: IrType
    ) : this(
        startOffset, endOffset, origin,
        IrSimpleFunctionSymbolImpl(descriptor), returnType
    )

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)
}
