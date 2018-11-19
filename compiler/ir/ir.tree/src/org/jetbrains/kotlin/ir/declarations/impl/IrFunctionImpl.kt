/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
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
    override val isSuspend: Boolean
) :
    IrFunctionBase(startOffset, endOffset, origin, name, visibility, isInline, isExternal, returnType),
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
        symbol.descriptor.isInline,
        symbol.descriptor.isExternal,
        symbol.descriptor.isTailrec,
        symbol.descriptor.isSuspend
    )

    override val descriptor: FunctionDescriptor = symbol.descriptor

    override val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> = SmartList()

    override var correspondingProperty: IrProperty? = null

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

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: FunctionDescriptor,
        returnType: IrType,
        body: IrBody?
    ) : this(startOffset, endOffset, origin, descriptor, returnType) {
        this.body = body
    }

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)
}