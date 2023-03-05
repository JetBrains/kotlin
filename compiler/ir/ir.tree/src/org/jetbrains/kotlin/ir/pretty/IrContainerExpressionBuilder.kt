/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType

abstract class IrContainerExpressionBuilder<ContainerExpression : IrContainerExpression> @PublishedApi internal constructor(buildingContext: IrBuildingContext) :
    IrExpressionBuilder<ContainerExpression>(buildingContext), IrStatementContainerBuilder {

    override val __internal_statementBuilders = mutableListOf<IrStatementBuilder<*>>()

    protected var statementOrigin: IrStatementOrigin? by SetAtMostOnce(null)

    @PrettyIrDsl
    fun origin(statementOrigin: IrStatementOrigin?) {
        this.statementOrigin = statementOrigin
    }
}

open class IrBlockBuilder @PublishedApi internal constructor(buildingContext: IrBuildingContext) :
    IrContainerExpressionBuilder<IrBlock>(buildingContext) {

    @PublishedApi
    override fun build(): IrBlock = IrBlockImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = statementOrigin,
        type = IrUninitializedType, // FIXME!!!
    ).also {
        addStatementsTo(it)
    }
}

class IrReturnableBlockBuilder @PublishedApi internal constructor(buildingContext: IrBuildingContext) : IrBlockBuilder(buildingContext), IrSymbolOwnerBuilder {

    override var symbolReference: String? by SetAtMostOnce(null)

    private var inlineFunctionSymbolReference: String? by SetAtMostOnce(null)

    @PrettyIrDsl
    fun inlineFunctionSymbol(symbolReference: String?) {
        inlineFunctionSymbolReference = symbolReference
    }

    @PublishedApi
    override fun build(): IrReturnableBlock = IrReturnableBlockImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = IrUninitializedType, // FIXME!!!,
        symbol = symbol(::IrReturnableBlockSymbolImpl),
        origin = statementOrigin,
        inlineFunctionSymbol = symbol(::IrSimpleFunctionSymbolImpl), // FIXME: Support public symbols (what about constructor symbols here???)
    ).also {
        addStatementsTo(it)
    }
}

class IrCompositeBuilder @PublishedApi internal constructor(buildingContext: IrBuildingContext) :
    IrContainerExpressionBuilder<IrComposite>(buildingContext) {

    @PublishedApi
    override fun build(): IrComposite = IrCompositeImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = IrUninitializedType, // FIXME!!!
        origin = statementOrigin
    ).also {
        addStatementsTo(it)
    }
}

