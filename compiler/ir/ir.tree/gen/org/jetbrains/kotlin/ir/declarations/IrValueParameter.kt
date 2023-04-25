/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.valueParameter]
 */
abstract class IrValueParameter : IrDeclarationBase(), IrValueDeclaration {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ParameterDescriptor

    abstract override val symbol: IrValueParameterSymbol

    abstract var index: Int

    abstract var varargElementType: IrType?

    abstract var isCrossinline: Boolean

    abstract var isNoinline: Boolean

    abstract var isHidden: Boolean

    abstract var defaultValue: IrExpressionBody?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitValueParameter(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D):
            IrValueParameter = accept(transformer, data) as IrValueParameter

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        defaultValue = defaultValue?.transform(transformer, data)
    }
}
