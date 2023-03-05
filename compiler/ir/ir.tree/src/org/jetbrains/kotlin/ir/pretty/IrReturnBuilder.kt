/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType

class IrReturnBuilder @PublishedApi internal constructor(buildingContext: IrBuildingContext) :
    IrExpressionBuilder<IrReturn>(buildingContext),
    IrExpressionContainerBuilder {

    override var __internal_expressionBuilder: IrExpressionBuilder<*>? by SetAtMostOnce(null)

    private var returnTargetSymbolReference: String? by SetAtMostOnce(null)

    @PrettyIrDsl
    fun from(symbolReference: String) {
        returnTargetSymbolReference = symbolReference
    }

    @PublishedApi
    override fun build(): IrReturn = IrReturnImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = IrUninitializedType, // FIXME!!!
        returnTargetSymbol = buildingContext.getSymbol(returnTargetSymbolReference ?: error("Missing return target symbol")),
        value = buildNonNullExpression()
    )
}
