/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.fir.expressions.LogicOperationKind

class ConeBinaryLogicExpression(val left: ConeBooleanExpression, val right: ConeBooleanExpression, val kind: LogicOperationKind) : ConeBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R {
        return contractDescriptionVisitor.visitLogicalBinaryOperationContractExpression(this, data)
    }
}

class ConeLogicalNot(val arg: ConeBooleanExpression) : ConeBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitLogicalNot(this, data)
}
