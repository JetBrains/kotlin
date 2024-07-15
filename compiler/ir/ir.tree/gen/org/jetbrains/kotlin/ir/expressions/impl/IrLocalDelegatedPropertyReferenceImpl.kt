/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrLocalDelegatedPropertyReferenceImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
    valueArguments: Array<IrExpression?>,
    typeArguments: Array<IrType?>,
    symbol: IrLocalDelegatedPropertySymbol,
    delegate: IrVariableSymbol,
    getter: IrSimpleFunctionSymbol,
    setter: IrSimpleFunctionSymbol?,
) : IrLocalDelegatedPropertyReference(
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
    valueArguments = valueArguments,
    typeArguments = typeArguments,
    symbol = symbol,
    delegate = delegate,
    getter = getter,
    setter = setter,
)
