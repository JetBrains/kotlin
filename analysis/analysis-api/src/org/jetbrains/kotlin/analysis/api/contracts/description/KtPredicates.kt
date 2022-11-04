/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.types.KtType

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.IsInstancePredicate]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeIsInstancePredicate]
 */
public class KtIsInstancePredicate(
    public val arg: KtValueParameterReference,
    public val type: KtType,
    public val isNegated: Boolean
) : KtBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitIsInstancePredicate(this, data)

    public fun negated(): KtIsInstancePredicate = KtIsInstancePredicate(arg, type, isNegated.not())
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.IsNullPredicate]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeIsNullPredicate]
 */
public class KtIsNullPredicate(public val arg: KtValueParameterReference, public val isNegated: Boolean) : KtBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitIsNullPredicate(this, data)

    public fun negated(): KtIsNullPredicate = KtIsNullPredicate(arg, isNegated.not())
}
