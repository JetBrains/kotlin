/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.contracts.description.KtContractConstantValue.KtContractConstantType
import org.jetbrains.kotlin.analysis.api.contracts.description.KtContractReturnsContractEffectDeclaration.*
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.symbols.KtParameterSymbol
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.contracts.description.expressions.*

internal fun EffectDeclaration.effectDeclarationToAnalysisApi(analysisContext: Fe10AnalysisContext): KtContractEffectDeclaration =
    accept(ContractDescriptionElementToAnalysisApi(analysisContext), Unit) as KtContractEffectDeclaration

private class ContractDescriptionElementToAnalysisApi(val analysisContext: Fe10AnalysisContext) :
    ContractDescriptionVisitor<Any, Unit> {

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConditionalEffectDeclaration,
        data: Unit
    ): Any = KtContractConditionalContractEffectDeclaration(
        conditionalEffect.effect.accept(),
        conditionalEffect.condition.accept(),
    )

    override fun visitReturnsEffectDeclaration(
        returnsEffect: ReturnsEffectDeclaration,
        data: Unit
    ): KtContractReturnsContractEffectDeclaration =
        when (val value = returnsEffect.value) {
            ConstantReference.NULL ->
                KtContractReturnsSpecificValueEffectDeclaration(KtContractConstantValue(KtContractConstantType.NULL, analysisContext.token))
            ConstantReference.NOT_NULL -> KtContractReturnsNotNullEffectDeclaration(analysisContext.token)
            ConstantReference.WILDCARD -> KtContractReturnsSuccessfullyEffectDeclaration(analysisContext.token)
            is BooleanConstantReference -> KtContractReturnsSpecificValueEffectDeclaration(
                KtContractConstantValue(
                    when (value) {
                        BooleanConstantReference.TRUE -> KtContractConstantType.TRUE
                        BooleanConstantReference.FALSE -> KtContractConstantType.FALSE
                        else -> error("Can't convert $value to the Analysis API")
                    },
                    analysisContext.token
                )
            )
            else -> error("Can't convert $returnsEffect to the Analysis API")
        }

    override fun visitCallsEffectDeclaration(callsEffect: CallsEffectDeclaration, data: Unit): Any =
        KtContractCallsInPlaceContractEffectDeclaration(callsEffect.variableReference.accept(), callsEffect.kind)

    override fun visitLogicalOr(logicalOr: LogicalOr, data: Unit): Any = KtContractBinaryLogicExpression(
        logicalOr.left.accept(),
        logicalOr.right.accept(),
        KtContractBinaryLogicExpression.KtLogicOperation.OR
    )

    override fun visitLogicalAnd(logicalAnd: LogicalAnd, data: Unit): Any = KtContractBinaryLogicExpression(
        logicalAnd.left.accept(),
        logicalAnd.right.accept(),
        KtContractBinaryLogicExpression.KtLogicOperation.AND
    )

    override fun visitLogicalNot(logicalNot: LogicalNot, data: Unit): Any =
        KtContractLogicalNotExpression(logicalNot.arg.accept())

    override fun visitIsInstancePredicate(isInstancePredicate: IsInstancePredicate, data: Unit): Any =
        KtContractIsInstancePredicateExpression(
            isInstancePredicate.arg.accept(),
            isInstancePredicate.type.toKtType(analysisContext),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: IsNullPredicate, data: Unit): Any =
        KtContractIsNullPredicateExpression(isNullPredicate.arg.accept(), isNullPredicate.isNegated)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: BooleanConstantReference,
        data: Unit
    ): KtContractBooleanConstantExpression =
        when (booleanConstantDescriptor) {
            BooleanConstantReference.TRUE -> KtContractBooleanConstantExpression(true, analysisContext.token)
            BooleanConstantReference.FALSE -> KtContractBooleanConstantExpression(false, analysisContext.token)
            else -> error("Can't convert $booleanConstantDescriptor to Analysis API")
        }

    override fun visitVariableReference(variableReference: VariableReference, data: Unit): Any =
        visitVariableReference(variableReference, ::KtContractParameterValue)

    override fun visitBooleanVariableReference(
        booleanVariableReference: BooleanVariableReference,
        data: Unit
    ): Any = visitVariableReference(booleanVariableReference, ::KtContractBooleanValueParameterExpression)

    private fun <T> visitVariableReference(
        variableReference: VariableReference,
        constructor: (KtParameterSymbol) -> T
    ): T = constructor(variableReference.descriptor.toKtSymbol(analysisContext) as KtParameterSymbol)

    // Util function to avoid hard coding names of the classes. Type inference will do a better job figuring out the best type to cast to.
    // This visitor isn't type-safe anyway
    private inline fun <reified T> ContractDescriptionElement.accept() = accept(this@ContractDescriptionElementToAnalysisApi, Unit) as T
}
