/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.allTypeParameters
import org.jetbrains.kotlin.ir.util.initializeParameterArguments
import org.jetbrains.kotlin.ir.util.initializeTypeArguments

class IrDelegatingConstructorCallImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int,
) : IrDelegatingConstructorCall() {
    override var origin: IrStatementOrigin? = null

    override val typeArguments: Array<IrType?> = initializeTypeArguments(typeArgumentsCount)

    override var dispatchReceiver: IrExpression? = null
    override var extensionReceiver: IrExpression? = null
    override val valueArguments: Array<IrExpression?> = initializeParameterArguments(valueArgumentsCount)

    override var contextReceiversCount = 0

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    companion object
}
