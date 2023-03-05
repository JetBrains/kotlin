/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

interface IrExpressionContainerBuilder {

    val buildingContext: IrBuildingContext

    @Suppress("PropertyName")
    var __internal_expressionBuilder: IrExpressionBuilder<*>?

    @Suppress("FunctionName")
    fun __internal_addExpressionBuilder(expressionBuilder: IrExpressionBuilder<*>) {
        __internal_expressionBuilder = expressionBuilder
    }
}

internal fun IrExpressionContainerBuilder.buildExpression() = __internal_expressionBuilder?.build()

internal fun IrExpressionContainerBuilder.buildNonNullExpression() = buildExpression() ?: error("Expected expression builder")

internal class IrExpressionContainerBuilderImpl(override val buildingContext: IrBuildingContext) : IrExpressionContainerBuilder {
    override var __internal_expressionBuilder: IrExpressionBuilder<*>? by SetAtMostOnce(null)
}
