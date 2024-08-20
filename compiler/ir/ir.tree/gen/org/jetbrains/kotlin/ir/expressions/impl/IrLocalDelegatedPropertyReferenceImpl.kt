/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
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
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var origin: IrStatementOrigin?,
    protected override val valueArguments: Array<IrExpression?>,
    protected override val typeArguments: Array<IrType?>,
    override var symbol: IrLocalDelegatedPropertySymbol,
    override var delegate: IrVariableSymbol,
    override var getter: IrSimpleFunctionSymbol,
    override var setter: IrSimpleFunctionSymbol?,
) : IrLocalDelegatedPropertyReference() {
    override var attributeOwnerId: IrAttributeContainer = this

    override var originalBeforeInline: IrAttributeContainer? = null

    override var dispatchReceiver: IrExpression? = null

    override var extensionReceiver: IrExpression? = null
}
