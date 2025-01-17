/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.types.Variance

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.typeParameter]
 */
abstract class IrTypeParameter : IrDeclarationBase(), IrDeclarationWithName {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: TypeParameterDescriptor

    abstract override val symbol: IrTypeParameterSymbol

    abstract var variance: Variance

    abstract var index: Int

    abstract var isReified: Boolean

    abstract var superTypes: List<IrType>

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitTypeParameter(this)
    }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrTypeParameter =
        accept(transformer, data) as IrTypeParameter

    override fun transformVoid(transformer: IrElementTransformerVoid): IrTypeParameter =
        transform(transformer, null) as IrTypeParameter
}
