/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeAliasSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

class IrTypeAliasImpl(
    startOffset: Int,
    endOffset: Int,
    override val symbol: IrTypeAliasSymbol,
    override val name: Name,
    override val visibility: Visibility,
    override val expandedType: IrType,
    override val isActual: Boolean,
    origin: IrDeclarationOrigin
) :
    IrDeclarationBase(startOffset, endOffset, origin),
    IrTypeAlias {

    init {
        symbol.bind(this)
    }

    override val descriptor: TypeAliasDescriptor
        get() = symbol.descriptor

    override val typeParameters: MutableList<IrTypeParameter> = SmartList()

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitTypeAlias(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        typeParameters.transform { it.transform(transformer, data) }
    }

    companion object {
        fun fromSymbolDescriptor(
            startOffset: Int,
            endOffset: Int,
            symbol: IrTypeAliasSymbol,
            expandedType: IrType,
            origin: IrDeclarationOrigin
        ) =
            IrTypeAliasImpl(
                startOffset, endOffset,
                symbol,
                symbol.descriptor.name,
                symbol.descriptor.visibility,
                expandedType,
                symbol.descriptor.isActual,
                origin
            )
    }
}