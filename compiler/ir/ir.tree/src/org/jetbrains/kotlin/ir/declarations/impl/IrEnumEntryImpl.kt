/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class IrEnumEntryImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrEnumEntrySymbol,
    override val name: Name
) : IrDeclarationBase(startOffset, endOffset, origin),
    IrEnumEntry {

    init {
        symbol.bind(this)
    }

    override val descriptor: ClassDescriptor get() = symbol.descriptor
    override var correspondingClass: IrClass? = null
    override var initializerExpression: IrExpression? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitEnumEntry(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializerExpression?.accept(visitor, data)
        correspondingClass?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        initializerExpression = initializerExpression?.transform(transformer, data)
        correspondingClass = correspondingClass?.transform(transformer, data) as? IrClass
    }
}

fun IrEnumEntryImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrEnumEntrySymbol
) =
    IrEnumEntryImpl(startOffset, endOffset, origin, symbol, symbol.descriptor.name)
