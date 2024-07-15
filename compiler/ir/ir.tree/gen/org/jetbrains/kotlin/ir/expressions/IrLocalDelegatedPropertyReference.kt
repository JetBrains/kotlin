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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.localDelegatedPropertyReference]
 */
abstract class IrLocalDelegatedPropertyReference(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
    valueArguments: Array<IrExpression?>,
    typeArguments: Array<IrType?>,
    override var symbol: IrLocalDelegatedPropertySymbol,
    var delegate: IrVariableSymbol,
    var getter: IrSimpleFunctionSymbol,
    var setter: IrSimpleFunctionSymbol?,
) : IrCallableReference<IrLocalDelegatedPropertySymbol>(
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
    valueArguments = valueArguments,
    typeArguments = typeArguments,
) {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitLocalDelegatedPropertyReference(this, data)
}
