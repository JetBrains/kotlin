/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrLocalDelegatedPropertySymbol,
    override val name: Name,
    override val type: IrType,
    override val isVar: Boolean
) :
    IrDeclarationBase(startOffset, endOffset, origin),
    IrLocalDelegatedProperty {

    init {
        symbol.bind(this)
    }

    override val descriptor: VariableDescriptorWithAccessors
        get() = symbol.descriptor

    override lateinit var delegate: IrVariable

    override lateinit var getter: IrFunction

    override var setter: IrFunction? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitLocalDelegatedProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        delegate.accept(visitor, data)
        getter.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        delegate = delegate.transform(transformer, data) as IrVariable
        getter = getter.transform(transformer, data) as IrFunction
        setter = setter?.transform(transformer, data) as? IrFunction
    }
}

fun IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrLocalDelegatedPropertySymbol,
    type: IrType
) =
    IrLocalDelegatedPropertyImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        type,
        symbol.descriptor.isVar
    )

@Deprecated("Creates unbound symbol", level = DeprecationLevel.ERROR)
fun IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptorWithAccessors,
    name: Name,
    type: IrType,
    isVar: Boolean
) =
    IrLocalDelegatedPropertyImpl(
        startOffset, endOffset, origin,
        IrLocalDelegatedPropertySymbolImpl(descriptor),
        name, type, isVar
    )

@Deprecated("Creates unbound symbol", level = DeprecationLevel.ERROR)
fun IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptorWithAccessors,
    type: IrType
) =
    IrLocalDelegatedPropertyImpl(
        startOffset, endOffset, origin,
        IrLocalDelegatedPropertySymbolImpl(descriptor),
        descriptor.name, type, descriptor.isVar
    )

@Suppress("DEPRECATION_ERROR")
@Deprecated("Creates unbound symbol", level = DeprecationLevel.ERROR)
fun IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptorWithAccessors,
    type: IrType,
    delegate: IrVariable,
    getter: IrFunction,
    setter: IrFunction?
): IrLocalDelegatedPropertyImpl =
    IrLocalDelegatedPropertyImpl(startOffset, endOffset, origin, descriptor, type).apply {
        this.delegate = delegate
        this.getter = getter
        this.setter = setter
    }
