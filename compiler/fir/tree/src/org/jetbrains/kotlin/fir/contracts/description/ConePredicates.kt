/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.fir.types.ConeKotlinType

class ConeIsInstancePredicate(val arg: ConeValueParameterReference, val type: ConeKotlinType, val isNegated: Boolean) : ConeBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitIsInstancePredicate(this, data)

    fun negated(): ConeIsInstancePredicate =
        ConeIsInstancePredicate(arg, type, isNegated.not())
}

class ConeIsNullPredicate(val arg: ConeValueParameterReference, val isNegated: Boolean) : ConeBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitIsNullPredicate(this, data)

    fun negated(): ConeIsNullPredicate =
        ConeIsNullPredicate(arg, isNegated.not())
}