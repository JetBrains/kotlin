/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.initializeParameterArguments
import org.jetbrains.kotlin.ir.util.initializeTypeArguments

class IrFunctionReferenceImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var symbol: IrFunctionSymbol,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int,
    override var reflectionTarget: IrFunctionSymbol? = symbol,
    override var origin: IrStatementOrigin? = null,
) : IrFunctionReference() {
    override val typeArguments: Array<IrType?> = initializeTypeArguments(typeArgumentsCount)

    override var dispatchReceiver: IrExpression? = null
    override var extensionReceiver: IrExpression? = null
    override val valueArguments: Array<IrExpression?> = initializeParameterArguments(valueArgumentsCount)

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    companion object
}