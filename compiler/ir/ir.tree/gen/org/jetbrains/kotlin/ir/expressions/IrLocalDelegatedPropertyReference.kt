/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.localDelegatedPropertyReference]
 */
abstract class IrLocalDelegatedPropertyReference : IrCallableReference<IrLocalDelegatedPropertySymbol>() {
    abstract var delegate: IrVariableSymbol

    abstract var getter: IrSimpleFunctionSymbol

    abstract var setter: IrSimpleFunctionSymbol?

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitLocalDelegatedPropertyReference(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitLocalDelegatedPropertyReference(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrExpression =
        transformer.visitLocalDelegatedPropertyReference(this, data) as IrExpression

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitLocalDelegatedPropertyReference(this) as IrExpression
}
