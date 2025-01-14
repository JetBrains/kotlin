/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.simpleFunction]
 */
abstract class IrSimpleFunction : IrFunction(), IrOverridableDeclaration<IrSimpleFunctionSymbol> {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: FunctionDescriptor

    abstract override val symbol: IrSimpleFunctionSymbol

    abstract override var overriddenSymbols: List<IrSimpleFunctionSymbol>

    abstract var isTailrec: Boolean

    abstract var isSuspend: Boolean

    abstract var isOperator: Boolean

    abstract var isInfix: Boolean

    abstract var correspondingPropertySymbol: IrPropertySymbol?

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitSimpleFunction(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrElement =
        transformer.visitSimpleFunction(this, data)

    override fun transformVoid(transformer: IrLeafTransformerVoid): IrElement =
        transformer.visitSimpleFunction(this)
}
