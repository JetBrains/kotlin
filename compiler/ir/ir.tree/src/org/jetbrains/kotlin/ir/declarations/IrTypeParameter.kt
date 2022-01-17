/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrAbstractVisitor
import org.jetbrains.kotlin.types.Variance

abstract class IrTypeParameter : IrDeclarationBase(), IrDeclarationWithName {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: TypeParameterDescriptor
    abstract override val symbol: IrTypeParameterSymbol

    abstract val variance: Variance
    abstract val index: Int
    abstract val isReified: Boolean
    abstract var superTypes: List<IrType>

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    override fun <R, D> accept(visitor: IrAbstractVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrTypeParameter =
        transformer.visitTypeParameter(this, data) as IrTypeParameter

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // no children
    }

    override fun <D> acceptChildren(visitor: IrAbstractVisitor<Unit, D>, data: D) {
        // no children
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // no children
    }
}
