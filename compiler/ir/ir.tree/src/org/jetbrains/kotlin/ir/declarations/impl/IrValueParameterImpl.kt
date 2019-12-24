/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class IrValueParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrValueParameterSymbol,
    override val name: Name,
    override val index: Int,
    override val type: IrType,
    override val varargElementType: IrType?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean
) :
    IrDeclarationBase(startOffset, endOffset, origin),
    IrValueParameter {

    override val descriptor: ParameterDescriptor = symbol.descriptor

    init {
        symbol.bind(this)
    }

    override var defaultValue: IrExpressionBody? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitValueParameter(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrValueParameter =
        transformer.visitValueParameter(this, data) as IrValueParameter

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        defaultValue = defaultValue?.transform(transformer, data)
    }
}

fun IrValueParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrValueParameterSymbol,
    type: IrType,
    varargElementType: IrType?
) =
    IrValueParameterImpl(
        startOffset, endOffset, origin,
        symbol,
        symbol.descriptor.name,
        symbol.descriptor.safeAs<ValueParameterDescriptor>()?.index ?: -1,
        type,
        varargElementType,
        symbol.descriptor.safeAs<ValueParameterDescriptor>()?.isCrossinline ?: false,
        symbol.descriptor.safeAs<ValueParameterDescriptor>()?.isNoinline ?: false
    )

fun IrValueParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: ParameterDescriptor,
    type: IrType,
    varargElementType: IrType?
) =
    IrValueParameterImpl(startOffset, endOffset, origin, IrValueParameterSymbolImpl(descriptor), type, varargElementType)
