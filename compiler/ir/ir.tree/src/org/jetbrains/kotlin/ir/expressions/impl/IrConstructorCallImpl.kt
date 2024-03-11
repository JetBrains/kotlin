/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.initializeParameterArguments
import org.jetbrains.kotlin.ir.util.initializeTypeArguments
import org.jetbrains.kotlin.ir.util.parentAsClass

class IrConstructorCallImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    override var constructorTypeArgumentsCount: Int,
    valueArgumentsCount: Int,
    override var origin: IrStatementOrigin? = null,
    override var source: SourceElement = SourceElement.NO_SOURCE
) : IrConstructorCall() {
    override val typeArguments: Array<IrType?> = initializeTypeArguments(typeArgumentsCount)

    override var dispatchReceiver: IrExpression? = null
    override var extensionReceiver: IrExpression? = null
    override val valueArguments: Array<IrExpression?> = initializeParameterArguments(valueArgumentsCount)

    override var contextReceiversCount = 0

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    companion object
}
