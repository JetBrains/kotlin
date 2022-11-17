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
    accept(ConeContractDescriptionElementToAnalysisApi(builder), Unit).cast()

private class ConeContractDescriptionElementToAnalysisApi(private val builder: KtSymbolByFirBuilder) :
    ConeContractDescriptionVisitor<KtContractDescriptionElement, Unit>() {

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConeConditionalEffectDeclaration,
        data: Unit
    ): KtContractDescriptionElement = KtConditionalEffectDeclaration(
        conditionalEffect.effect.accept(this, data).cast(),
        conditionalEffect.condition.accept(this, data).cast()
    )

    override fun visitReturnsEffectDeclaration(returnsEffect: ConeReturnsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtReturnsEffectDeclaration(returnsEffect.value.accept(this, data).cast())

    override fun visitCallsEffectDeclaration(callsEffect: ConeCallsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtCallsEffectDeclaration(
            callsEffect.valueParameterReference.accept(this, data).cast(),
            callsEffect.kind
        )

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: ConeBinaryLogicExpression,
        data: Unit
    ): KtContractDescriptionElement = KtBinaryLogicExpression(
        binaryLogicExpression.left.accept(this, data).cast(),
        binaryLogicExpression.right.accept(this, data).cast(),
        when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> KtBinaryLogicExpression.KtLogicOperationKind.AND
            LogicOperationKind.OR -> KtBinaryLogicExpression.KtLogicOperationKind.OR
        }
    )

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Unit): KtContractDescriptionElement =
        KtLogicalNot(logicalNot.arg.accept(this, data).cast())

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Unit): KtContractDescriptionElement =
        KtIsInstancePredicate(
            isInstancePredicate.arg.accept(this, data).cast(),
            builder.typeBuilder.buildKtType(isInstancePredicate.type),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Unit): KtContractDescriptionElement =
        KtIsNullPredicate(isNullPredicate.arg.accept(this, data).cast(), isNullPredicate.isNegated)

    override fun visitConstantDescriptor(constantReference: ConeConstantReference, data: Unit): KtContractDescriptionElement =
        KtAbstractConstantReference.KtConstantReference(constantReference.name, builder.token)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: ConeBooleanConstantReference,
        data: Unit
    ): KtContractDescriptionElement = KtAbstractConstantReference.KtBooleanConstantReference(booleanConstantDescriptor.name, builder.token)

    override fun visitValueParameterReference(
        valueParameterReference: ConeValueParameterReference,
        data: Unit
    ): KtContractDescriptionElement = KtAbstractValueParameterReference.KtValueParameterReference(
        valueParameterReference.parameterIndex,
        valueParameterReference.name,
        builder.token
    )

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: ConeBooleanValueParameterReference,
        data: Unit
    ): KtContractDescriptionElement = KtAbstractValueParameterReference.KtBooleanValueParameterReference(
        booleanValueParameterReference.parameterIndex,
        booleanValueParameterReference.name,
        builder.token
    )
}

// Util function to avoid hard coding names of the classes. Type inference will do a better job figuring out the best type to cast to.
// This visitor isn't type-safe anyway
private inline fun <reified T : KtContractDescriptionElement> Any.cast() = this as T
