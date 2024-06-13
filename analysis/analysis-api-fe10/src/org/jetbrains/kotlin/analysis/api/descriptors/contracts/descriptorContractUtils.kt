/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConstantValue.KaContractConstantType
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractReturnsContractEffectDeclaration.*
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.contracts.description.expressions.*

internal fun EffectDeclaration.effectDeclarationToAnalysisApi(analysisContext: Fe10AnalysisContext): KaContractEffectDeclaration =
    accept(ContractDescriptionElementToAnalysisApi(analysisContext), Unit) as KaContractEffectDeclaration

private class ContractDescriptionElementToAnalysisApi(val analysisContext: Fe10AnalysisContext) :
    ContractDescriptionVisitor<Any, Unit> {

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConditionalEffectDeclaration,
        data: Unit
    ): Any = KaContractConditionalContractEffectDeclaration(
        conditionalEffect.effect.accept(),
        conditionalEffect.condition.accept(),
    )

    override fun visitReturnsEffectDeclaration(
        returnsEffect: ReturnsEffectDeclaration,
        data: Unit
    ): KaContractReturnsContractEffectDeclaration =
        when (val value = returnsEffect.value) {
            ConstantReference.NULL ->
                KaContractReturnsSpecificValueEffectDeclaration(KaContractConstantValue(KaContractConstantType.NULL, analysisContext.token))
            ConstantReference.NOT_NULL -> KaContractReturnsNotNullEffectDeclaration(analysisContext.token)
            ConstantReference.WILDCARD -> KaContractReturnsSuccessfullyEffectDeclaration(analysisContext.token)
            is BooleanConstantReference -> KaContractReturnsSpecificValueEffectDeclaration(
                KaContractConstantValue(
                    when (value) {
                        BooleanConstantReference.TRUE -> KaContractConstantType.TRUE
                        BooleanConstantReference.FALSE -> KaContractConstantType.FALSE
                        else -> error("Can't convert $value to the Analysis API")
                    },
                    analysisContext.token
                )
            )
            else -> error("Can't convert $returnsEffect to the Analysis API")
        }

    override fun visitCallsEffectDeclaration(callsEffect: CallsEffectDeclaration, data: Unit): Any =
        KaContractCallsInPlaceContractEffectDeclaration(callsEffect.variableReference.accept(), callsEffect.kind)

    override fun visitLogicalOr(logicalOr: LogicalOr, data: Unit): Any = KaContractBinaryLogicExpression(
        logicalOr.left.accept(),
        logicalOr.right.accept(),
        KaContractBinaryLogicExpression.KaLogicOperation.OR
    )

    override fun visitLogicalAnd(logicalAnd: LogicalAnd, data: Unit): Any = KaContractBinaryLogicExpression(
        logicalAnd.left.accept(),
        logicalAnd.right.accept(),
        KaContractBinaryLogicExpression.KaLogicOperation.AND
    )

    override fun visitLogicalNot(logicalNot: LogicalNot, data: Unit): Any =
        KaContractLogicalNotExpression(logicalNot.arg.accept())

    override fun visitIsInstancePredicate(isInstancePredicate: IsInstancePredicate, data: Unit): Any =
        KaContractIsInstancePredicateExpression(
            isInstancePredicate.arg.accept(),
            isInstancePredicate.type.toKtType(analysisContext),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: IsNullPredicate, data: Unit): Any =
        KaContractIsNullPredicateExpression(isNullPredicate.arg.accept(), isNullPredicate.isNegated)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: BooleanConstantReference,
        data: Unit
    ): KaContractBooleanConstantExpression =
        when (booleanConstantDescriptor) {
            BooleanConstantReference.TRUE -> KaContractBooleanConstantExpression(true, analysisContext.token)
            BooleanConstantReference.FALSE -> KaContractBooleanConstantExpression(false, analysisContext.token)
            else -> error("Can't convert $booleanConstantDescriptor to Analysis API")
        }

    override fun visitVariableReference(variableReference: VariableReference, data: Unit): Any =
        visitVariableReference(variableReference, ::KaContractParameterValue)

    override fun visitBooleanVariableReference(
        booleanVariableReference: BooleanVariableReference,
        data: Unit
    ): Any = visitVariableReference(booleanVariableReference, ::KaContractBooleanValueParameterExpression)

    private fun <T> visitVariableReference(
        variableReference: VariableReference,
        constructor: (KaParameterSymbol) -> T
    ): T = constructor(variableReference.descriptor.toKtSymbol(analysisContext) as KaParameterSymbol)

    // Util function to avoid hard coding names of the classes. Type inference will do a better job figuring out the best type to cast to.
    // This visitor isn't type-safe anyway
    private inline fun <reified T> ContractDescriptionElement.accept() = accept(this@ContractDescriptionElementToAnalysisApi, Unit) as T
}
