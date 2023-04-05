/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType

object ConeContractConstantValues {
    val NULL = KtConstantReference<ConeKotlinType, ConeDiagnostic>("NULL")
    val WILDCARD = KtConstantReference<ConeKotlinType, ConeDiagnostic>("WILDCARD")
    val NOT_NULL = KtConstantReference<ConeKotlinType, ConeDiagnostic>("NOT_NULL")
    val TRUE = KtBooleanConstantReference<ConeKotlinType, ConeDiagnostic>("TRUE")
    val FALSE = KtBooleanConstantReference<ConeKotlinType, ConeDiagnostic>("FALSE")
}

typealias ConeEffectDeclaration = KtEffectDeclaration<ConeKotlinType, ConeDiagnostic>
typealias ConeContractDescriptionElement = KtContractDescriptionElement<ConeKotlinType, ConeDiagnostic>
typealias ConeCallsEffectDeclaration = KtCallsEffectDeclaration<ConeKotlinType, ConeDiagnostic>
typealias ConeConditionalEffectDeclaration = KtConditionalEffectDeclaration<ConeKotlinType, ConeDiagnostic>
typealias ConeReturnsEffectDeclaration = KtReturnsEffectDeclaration<ConeKotlinType, ConeDiagnostic>
typealias ConeConstantReference = KtConstantReference<ConeKotlinType, ConeDiagnostic>
typealias ConeIsNullPredicate = KtIsNullPredicate<ConeKotlinType, ConeDiagnostic>
typealias ConeIsInstancePredicate = KtIsInstancePredicate<ConeKotlinType, ConeDiagnostic>
typealias ConeLogicalNot = KtLogicalNot<ConeKotlinType, ConeDiagnostic>
typealias ConeBooleanExpression = KtBooleanExpression<ConeKotlinType, ConeDiagnostic>
typealias ConeBinaryLogicExpression = KtBinaryLogicExpression<ConeKotlinType, ConeDiagnostic>
typealias ConeBooleanConstantReference = KtBooleanConstantReference<ConeKotlinType, ConeDiagnostic>
typealias ConeValueParameterReference = KtValueParameterReference<ConeKotlinType, ConeDiagnostic>
typealias ConeBooleanValueParameterReference = KtBooleanValueParameterReference<ConeKotlinType, ConeDiagnostic>
