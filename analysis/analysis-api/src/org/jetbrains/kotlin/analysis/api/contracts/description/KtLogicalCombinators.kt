/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.LogicalOr] & [org.jetbrains.kotlin.contracts.description.expressions.LogicalAnd]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBinaryLogicExpression]
 */
public class KtBinaryLogicExpression(
    public val left: KtBooleanExpression,
    public val right: KtBooleanExpression,
    public val kind: LogicOperationKind
) : KtBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitLogicalBinaryOperationContractExpression(this, data)
}

public enum class LogicOperationKind(public val token: String) {
    AND("&&"), OR("||")
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.LogicalNot]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeLogicalNot]
 */
public class KtLogicalNot(public val arg: KtBooleanExpression) : KtBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitLogicalNot(this, data)
}
