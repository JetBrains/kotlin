/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.contracts.description.KtContractConstantReference.KtContractBooleanConstantReference
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.expressions.LogicOperationKind

internal fun ConeEffectDeclaration.coneEffectDeclarationToAnalysisApi(builder: KtSymbolByFirBuilder): KtContractEffectDeclaration =
    accept(ConeContractDescriptionElementToAnalysisApi(builder), Unit).cast()

private class ConeContractDescriptionElementToAnalysisApi(private val builder: KtSymbolByFirBuilder) :
    ConeContractDescriptionVisitor<KtContractDescriptionElement, Unit>() {

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConeConditionalEffectDeclaration,
        data: Unit
    ): KtContractDescriptionElement = KtContractConditionalContractEffectDeclaration(
        conditionalEffect.effect.accept(this, data).cast(),
        conditionalEffect.condition.accept(this, data).cast()
    )

    override fun visitReturnsEffectDeclaration(returnsEffect: ConeReturnsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtContractReturnsContractEffectDeclaration(returnsEffect.value.accept(this, data).cast())

    override fun visitCallsEffectDeclaration(callsEffect: ConeCallsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtContractCallsContractEffectDeclaration(
            callsEffect.valueParameterReference.accept(this, data).cast(),
            callsEffect.kind
        )

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: ConeBinaryLogicExpression,
        data: Unit
    ): KtContractDescriptionElement = KtContractBinaryLogicExpression(
        binaryLogicExpression.left.accept(this, data).cast(),
        binaryLogicExpression.right.accept(this, data).cast(),
        when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> KtContractBinaryLogicExpression.KtLogicOperationKind.AND
            LogicOperationKind.OR -> KtContractBinaryLogicExpression.KtLogicOperationKind.OR
        }
    )

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Unit): KtContractDescriptionElement =
        KtContractLogicalNot(logicalNot.arg.accept(this, data).cast())

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Unit): KtContractDescriptionElement =
        KtContractIsInstancePredicate(
            isInstancePredicate.arg.accept(this, data).cast(),
            builder.typeBuilder.buildKtType(isInstancePredicate.type),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Unit): KtContractDescriptionElement =
        KtContractIsNullPredicate(isNullPredicate.arg.accept(this, data).cast(), isNullPredicate.isNegated)

    override fun visitConstantDescriptor(constantReference: ConeConstantReference, data: Unit): KtContractDescriptionElement =
        when (constantReference) {
            ConeConstantReference.NULL -> KtContractConstantReference.KtNull(builder.token)
            ConeConstantReference.NOT_NULL -> KtContractConstantReference.KtNotNull(builder.token)
            ConeConstantReference.WILDCARD -> KtContractConstantReference.KtWildcard(builder.token)
            else -> error("Can't convert $constantReference to Analysis API")
        }

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: ConeBooleanConstantReference,
        data: Unit
    ): KtContractDescriptionElement =
        when (booleanConstantDescriptor) {
            ConeBooleanConstantReference.TRUE -> KtContractBooleanConstantReference.KtTrue(builder.token)
            ConeBooleanConstantReference.FALSE -> KtContractBooleanConstantReference.KtFalse(builder.token)
            else -> error("Can't convert $booleanConstantDescriptor to Analysis API")
        }

    override fun visitValueParameterReference(
        valueParameterReference: ConeValueParameterReference,
        data: Unit
    ): KtContractDescriptionElement = KtContractAbstractValueParameterReference.KtContractValueParameterReference(
        valueParameterReference.parameterIndex,
        valueParameterReference.name,
        builder.token
    )

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: ConeBooleanValueParameterReference,
        data: Unit
    ): KtContractDescriptionElement = KtContractAbstractValueParameterReference.KtContractBooleanValueParameterReference(
        booleanValueParameterReference.parameterIndex,
        booleanValueParameterReference.name,
        builder.token
    )
}

// Util function to avoid hard coding names of the classes. Type inference will do a better job figuring out the best type to cast to.
// This visitor isn't type-safe anyway
private inline fun <reified T : KtContractDescriptionElement> Any.cast() = this as T
