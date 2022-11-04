/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.expressions.LogicOperationKind

internal fun ConeEffectDeclaration.coneEffectDeclarationToAnalysisApi(builder: KtSymbolByFirBuilder): KtEffectDeclaration =
    accept(ConeContractDescriptionElementToAnalysisApi(builder), Unit) as KtEffectDeclaration

private class ConeContractDescriptionElementToAnalysisApi(private val builder: KtSymbolByFirBuilder) : ConeContractDescriptionVisitor<KtContractDescriptionElement, Unit>() {
    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConeConditionalEffectDeclaration,
        data: Unit
    ): KtContractDescriptionElement = KtConditionalEffectDeclaration(
        conditionalEffect.effect.accept(this, data) as KtEffectDeclaration,
        conditionalEffect.condition.accept(this, data) as KtBooleanExpression
    )

    override fun visitReturnsEffectDeclaration(returnsEffect: ConeReturnsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtReturnsEffectDeclaration(returnsEffect.value.accept(this, data) as KtConstantReference)

    override fun visitCallsEffectDeclaration(callsEffect: ConeCallsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtCallsEffectDeclaration(
            callsEffect.valueParameterReference.accept(this, data) as KtValueParameterReference,
            callsEffect.kind
        )

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: ConeBinaryLogicExpression,
        data: Unit
    ): KtContractDescriptionElement = KtBinaryLogicExpression(
        binaryLogicExpression.left.accept(this, data) as KtBooleanExpression,
        binaryLogicExpression.right.accept(this, data) as KtBooleanExpression,
        when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> org.jetbrains.kotlin.analysis.api.contracts.description.LogicOperationKind.AND
            LogicOperationKind.OR -> org.jetbrains.kotlin.analysis.api.contracts.description.LogicOperationKind.OR
        }
    )

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Unit): KtContractDescriptionElement =
        KtLogicalNot(logicalNot.arg.accept(this, data) as KtBooleanExpression)

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Unit): KtContractDescriptionElement =
        KtIsInstancePredicate(
            isInstancePredicate.arg.accept(this, data) as KtValueParameterReference,
            builder.typeBuilder.buildKtType(isInstancePredicate.type),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Unit): KtContractDescriptionElement =
        KtIsNullPredicate(isNullPredicate.arg.accept(this, data) as KtValueParameterReference, isNullPredicate.isNegated)

    override fun visitConstantDescriptor(constantReference: ConeConstantReference, data: Unit): KtContractDescriptionElement =
        KtConstantReference(constantReference.name)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: ConeBooleanConstantReference,
        data: Unit
    ): KtContractDescriptionElement = KtBooleanConstantReference(booleanConstantDescriptor.name)

    override fun visitValueParameterReference(
        valueParameterReference: ConeValueParameterReference,
        data: Unit
    ): KtContractDescriptionElement = KtValueParameterReference(valueParameterReference.name)

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: ConeBooleanValueParameterReference,
        data: Unit
    ): KtContractDescriptionElement = KtBooleanValueParameterReference(booleanValueParameterReference.name)
}
