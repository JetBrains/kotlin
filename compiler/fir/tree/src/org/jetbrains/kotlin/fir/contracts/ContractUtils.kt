/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.builder.buildEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.expressions.LogicOperationKind
import org.jetbrains.kotlin.fir.types.ConeKotlinType

val FirContractDescription.effects: List<FirEffectDeclaration>?
    get() = (this as? FirResolvedContractDescription)?.effects

val FirContractDescription.coneEffects: List<ConeEffectDeclaration>?
    get() = effects?.map { it.effect }

val FirResolvedContractDescription.resolvedConeEffects: List<ConeEffectDeclaration>
    get() = effects.map { it.effect }

fun ConeEffectDeclaration.toFirEffectDeclaration(source: FirSourceElement? = null): FirEffectDeclaration =
    buildEffectDeclaration {
        if (source != null) {
            this.source = source
        }
        effect = this@toFirEffectDeclaration
    }

fun ConeConditionalEffectDeclaration.collectReturnValueConditionalTypes(
    conditionalTypes: MutableList<ConeKotlinType>,
    builtinTypes: BuiltinTypes
): MutableList<ConeKotlinType> {
    if (this.effect is ConeParametersEffectDeclaration) {
        fun ConeBooleanExpression.conditionalTypes() {
            when (this) {
                is ConeBinaryLogicExpression -> if (kind == LogicOperationKind.AND) {
                    left.conditionalTypes()
                    right.conditionalTypes()
                }
                is ConeIsInstancePredicate -> if (arg is ConeReturnValue && !isNegated) conditionalTypes += type
                is ConeIsNullPredicate -> if (arg is ConeReturnValue && isNegated) conditionalTypes += builtinTypes.anyType.type
            }
        }
        condition.conditionalTypes()
    }
    return conditionalTypes
}