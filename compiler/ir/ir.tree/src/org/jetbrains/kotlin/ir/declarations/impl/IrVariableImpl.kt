/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class IrVariableImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrVariableSymbol,
    override val name: Name,
    override val type: IrType,
    override val isVar: Boolean,
    override val isConst: Boolean,
    override val isLateinit: Boolean
) :
    IrDeclarationBase(startOffset, endOffset, origin),
    IrVariable {

    init {
        symbol.bind(this)
    }

    override val descriptor: VariableDescriptor get() = symbol.descriptor

    override var initializer: IrExpression? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitVariable(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializer?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        initializer = initializer?.transform(transformer, data)
    }
}

fun IrVariableImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrVariableSymbol,
    type: IrType
) =
    IrVariableImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name, type,
        isVar = symbol.descriptor.isVar,
        isConst = symbol.descriptor.isConst,
        isLateinit = symbol.descriptor.isLateInit
    )

fun IrVariableImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptor,
    type: IrType
) =
    IrVariableImpl(startOffset, endOffset, origin, IrVariableSymbolImpl(descriptor), type)

fun IrVariableImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptor,
    type: IrType,
    initializer: IrExpression?
) =
    IrVariableImpl(startOffset, endOffset, origin, descriptor, type).apply {
        this.initializer = initializer
    }
