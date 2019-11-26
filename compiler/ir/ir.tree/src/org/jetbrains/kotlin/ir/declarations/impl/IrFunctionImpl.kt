/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

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
    override val isFakeOverride: Boolean
) :
    IrFunctionBase(startOffset, endOffset, origin, name, visibility, isInline, isExternal, isExpect, returnType),
    IrSimpleFunction {

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

    override val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> = SmartList()

    override var correspondingPropertySymbol: IrPropertySymbol? = null

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
