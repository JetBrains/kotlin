/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrAbstractVisitor

abstract class IrValueParameter : IrValueDeclaration() {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ParameterDescriptor
    abstract override val symbol: IrValueParameterSymbol

    abstract val index: Int
    abstract var varargElementType: IrType?
    abstract val isCrossinline: Boolean
    abstract val isNoinline: Boolean

    // if true parameter is not included into IdSignature.
    // Skipping hidden params makes IrFunction be look similar to FE.
    // NOTE: it is introduced to fix KT-40980 because more clear solution was not possible to implement.
    // Once we are able to load any top-level declaration from klib this hack should be deprecated and removed.
    abstract val isHidden: Boolean

    abstract var defaultValue: IrExpressionBody?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitValueParameter(this, data)

    override fun <R, D> accept(visitor: IrAbstractVisitor<R, D>, data: D): R =
        visitor.visitValueParameter(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrValueParameter =
        transformer.visitValueParameter(this, data) as IrValueParameter

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        defaultValue?.accept(visitor, data)
    }

    override fun <D> acceptChildren(visitor: IrAbstractVisitor<Unit, D>, data: D) {
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        defaultValue = defaultValue?.transform(transformer, data)
    }
}
