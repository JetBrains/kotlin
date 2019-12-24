/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.SmartList

class IrTypeParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrTypeParameterSymbol,
    override val name: Name,
    override val index: Int,
    override val isReified: Boolean,
    override val variance: Variance
) :
    IrDeclarationBase(startOffset, endOffset, origin),
    IrTypeParameter {

    init {
        symbol.bind(this)
    }

    override val descriptor: TypeParameterDescriptor get() = symbol.descriptor

    override val superTypes: MutableList<IrType> = SmartList()

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrTypeParameter =
        transformer.visitTypeParameter(this, data) as IrTypeParameter

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // no children
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // no children
    }
}

fun IrTypeParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrTypeParameterSymbol
) =
    IrTypeParameterImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        symbol.descriptor.index,
        symbol.descriptor.isReified,
        symbol.descriptor.variance
    )

fun IrTypeParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrTypeParameterSymbol,
    name: Name,
    index: Int,
    variance: Variance
) =
    IrTypeParameterImpl(startOffset, endOffset, origin, symbol, name, index, symbol.descriptor.isReified, variance)

@Deprecated("Use constructor which takes symbol instead of descriptor", level = DeprecationLevel.ERROR)
fun IrTypeParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: TypeParameterDescriptor
) =
    IrTypeParameterImpl(startOffset, endOffset, origin, IrTypeParameterSymbolImpl(descriptor))
