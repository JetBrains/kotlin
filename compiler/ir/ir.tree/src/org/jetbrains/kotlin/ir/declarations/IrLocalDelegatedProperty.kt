/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrLocalDelegatedProperty :
    IrDeclarationBase(),
    IrDeclarationWithName,
    IrSymbolOwner,
    IrMetadataSourceOwner {

    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: VariableDescriptorWithAccessors
    abstract override val symbol: IrLocalDelegatedPropertySymbol

    abstract var type: IrType
    abstract val isVar: Boolean

    abstract var delegate: IrVariable
    abstract var getter: IrSimpleFunction
    abstract var setter: IrSimpleFunction?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitLocalDelegatedProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        delegate.accept(visitor, data)
        getter.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        delegate = delegate.transform(transformer, data) as IrVariable
        getter = getter.transform(transformer, data) as IrSimpleFunction
        setter = setter?.transform(transformer, data) as? IrSimpleFunction
    }
}
