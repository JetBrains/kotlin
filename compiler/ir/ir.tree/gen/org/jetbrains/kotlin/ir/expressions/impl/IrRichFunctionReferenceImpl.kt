/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.IrSourceElement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrRichFunctionReferenceImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    override val sourceLocation: IrSourceElement,
    override var type: IrType,
    override var reflectionTargetSymbol: IrFunctionSymbol?,
    override var overriddenFunctionSymbol: IrSimpleFunctionSymbol,
    override var invokeFunction: IrSimpleFunction,
    override var origin: IrStatementOrigin?,
    override var hasUnitConversion: Boolean,
    override var hasSuspendConversion: Boolean,
    override var hasVarargConversion: Boolean,
    override var isRestrictedSuspension: Boolean,
) : IrRichFunctionReference() {
    override var attributeOwnerId: IrElement = this

    override val boundValues: MutableList<IrExpression> = ArrayList()
}
