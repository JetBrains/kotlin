/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor

val FirContractDescription?.isNullOrEmpty: Boolean
    get() = (this == null) || (this is FirEmptyContractDescription)

private fun ConeEffectDeclaration.substitute(substitutor: ConeSubstitutor): ConeEffectDeclaration {
    return when (this) {
        is ConeConditionalEffectDeclaration -> ConeConditionalEffectDeclaration(
            effect.substitute(substitutor),
            condition.substitute(substitutor)
        )
        is ConeCallsEffectDeclaration -> this
        is ConeReturnsEffectDeclaration -> this
        else -> error("Unknown effect declaration: $this")
    }
}

private fun ConeBooleanExpression.substitute(substitutor: ConeSubstitutor): ConeBooleanExpression {
    return when (this) {
        is ConeIsInstancePredicate -> {
            val newType = substitutor.substituteOrNull(type) ?: return this
            ConeIsInstancePredicate(arg, newType, isNegated)
        }
        is ConeBinaryLogicExpression -> ConeBinaryLogicExpression(left.substitute(substitutor), right.substitute(substitutor), kind)
        is ConeBooleanConstantReference -> this
        is ConeBooleanValueParameterReference -> this
        is ConeIsNullPredicate -> this
        is ConeLogicalNot -> ConeLogicalNot(arg.substitute(substitutor))
        else -> error("Unknown effect expression: $this")
    }
}
