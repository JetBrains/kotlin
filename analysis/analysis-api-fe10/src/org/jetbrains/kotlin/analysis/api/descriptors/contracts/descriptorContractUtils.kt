/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.contracts.description.expressions.*

internal fun EffectDeclaration.effectDeclarationToAnalysisApi(analysisContext: Fe10AnalysisContext): KtEffectDeclaration =
    accept(ContractDescriptionElementToAnalysisApi(analysisContext), Unit) as KtEffectDeclaration

private class ContractDescriptionElementToAnalysisApi(val analysisContext: Fe10AnalysisContext) :
    ContractDescriptionVisitor<KtContractDescriptionElement, Unit> {

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConditionalEffectDeclaration,
        data: Unit
    ): KtContractDescriptionElement = KtConditionalEffectDeclaration(
        conditionalEffect.effect.accept(this, data) as KtEffectDeclaration,
        conditionalEffect.condition.accept(this, data) as KtBooleanExpression,
    )

    override fun visitReturnsEffectDeclaration(returnsEffect: ReturnsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtReturnsEffectDeclaration(returnsEffect.value.accept(this, data) as KtConstantReference)

    override fun visitCallsEffectDeclaration(callsEffect: CallsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtCallsEffectDeclaration(callsEffect.variableReference.accept(this, data) as KtValueParameterReference, callsEffect.kind)

    override fun visitLogicalOr(logicalOr: LogicalOr, data: Unit): KtContractDescriptionElement = KtBinaryLogicExpression(
        logicalOr.left.accept(this, data) as KtBooleanExpression,
        logicalOr.right.accept(this, data) as KtBooleanExpression,
        LogicOperationKind.OR
    )

    override fun visitLogicalAnd(logicalAnd: LogicalAnd, data: Unit): KtContractDescriptionElement = KtBinaryLogicExpression(
        logicalAnd.left.accept(this, data) as KtBooleanExpression,
        logicalAnd.right.accept(this, data) as KtBooleanExpression,
        LogicOperationKind.AND
    )

    override fun visitLogicalNot(logicalNot: LogicalNot, data: Unit): KtContractDescriptionElement =
        KtLogicalNot(logicalNot.arg.accept(this, data) as KtBooleanExpression)

    override fun visitIsInstancePredicate(isInstancePredicate: IsInstancePredicate, data: Unit): KtContractDescriptionElement =
        KtIsInstancePredicate(
            isInstancePredicate.arg.accept(this, data) as KtValueParameterReference,
            isInstancePredicate.type.toKtType(analysisContext),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: IsNullPredicate, data: Unit): KtContractDescriptionElement =
        KtIsNullPredicate(isNullPredicate.arg.accept(this, data) as KtValueParameterReference, isNullPredicate.isNegated)

    override fun visitConstantDescriptor(constantReference: ConstantReference, data: Unit): KtContractDescriptionElement =
        KtConstantReference(constantReference.name)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: BooleanConstantReference,
        data: Unit
    ): KtContractDescriptionElement = KtBooleanConstantReference(booleanConstantDescriptor.name)

    override fun visitVariableReference(variableReference: VariableReference, data: Unit): KtContractDescriptionElement =
        KtValueParameterReference(variableReference.descriptor.name.asString())

    override fun visitBooleanVariableReference(
        booleanVariableReference: BooleanVariableReference,
        data: Unit
    ): KtContractDescriptionElement = KtBooleanValueParameterReference(booleanVariableReference.descriptor.name.asString())
}
