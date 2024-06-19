/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConstantValue.KaContractConstantType
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.KaBaseContractCallsInPlaceContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.KaBaseContractConditionalContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.KaBaseContractConstantValue
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.KaBaseContractParameterValue
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.KaBaseContractReturnsContractEffectDeclarations.KaBaseContractReturnsNotNullEffectDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.KaBaseContractReturnsContractEffectDeclarations.KaBaseContractReturnsSpecificValueEffectDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.KaBaseContractReturnsContractEffectDeclarations.KaBaseContractReturnsSuccessfullyEffectDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.booleans.KaBaseContractBinaryLogicExpression
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.booleans.KaBaseContractBooleanConstantExpression
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.booleans.KaBaseContractBooleanValueParameterExpression
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.booleans.KaBaseContractIsInstancePredicateExpression
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.booleans.KaBaseContractIsNullPredicateExpression
import org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.booleans.KaBaseContractLogicalNotExpression
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
    ): Any = KaBaseContractConditionalContractEffectDeclaration(
        conditionalEffect.effect.accept(),
        conditionalEffect.condition.accept(),
    )

    override fun visitReturnsEffectDeclaration(
        returnsEffect: ReturnsEffectDeclaration,
        data: Unit
    ): KaContractReturnsContractEffectDeclaration =
        when (val value = returnsEffect.value) {
            ConstantReference.NULL -> KaBaseContractReturnsSpecificValueEffectDeclaration(
                KaBaseContractConstantValue(
                    KaContractConstantType.NULL,
                    analysisContext.token,
                )
            )
            ConstantReference.NOT_NULL -> KaBaseContractReturnsNotNullEffectDeclaration(analysisContext.token)
            ConstantReference.WILDCARD -> KaBaseContractReturnsSuccessfullyEffectDeclaration(analysisContext.token)
            is BooleanConstantReference -> KaBaseContractReturnsSpecificValueEffectDeclaration(
                KaBaseContractConstantValue(
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
        KaBaseContractCallsInPlaceContractEffectDeclaration(callsEffect.variableReference.accept(), callsEffect.kind)

    override fun visitLogicalOr(logicalOr: LogicalOr, data: Unit): Any = KaBaseContractBinaryLogicExpression(
        logicalOr.left.accept(),
        logicalOr.right.accept(),
        KaContractBinaryLogicExpression.KaLogicOperation.OR
    )

    override fun visitLogicalAnd(logicalAnd: LogicalAnd, data: Unit): Any = KaBaseContractBinaryLogicExpression(
        logicalAnd.left.accept(),
        logicalAnd.right.accept(),
        KaContractBinaryLogicExpression.KaLogicOperation.AND
    )

    override fun visitLogicalNot(logicalNot: LogicalNot, data: Unit): Any =
        KaBaseContractLogicalNotExpression(logicalNot.arg.accept())

    override fun visitIsInstancePredicate(isInstancePredicate: IsInstancePredicate, data: Unit): Any =
        KaBaseContractIsInstancePredicateExpression(
            isInstancePredicate.arg.accept(),
            isInstancePredicate.type.toKtType(analysisContext),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: IsNullPredicate, data: Unit): Any =
        KaBaseContractIsNullPredicateExpression(isNullPredicate.arg.accept(), isNullPredicate.isNegated)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: BooleanConstantReference,
        data: Unit
    ): KaContractBooleanConstantExpression =
        when (booleanConstantDescriptor) {
            BooleanConstantReference.TRUE -> KaBaseContractBooleanConstantExpression(true, analysisContext.token)
            BooleanConstantReference.FALSE -> KaBaseContractBooleanConstantExpression(false, analysisContext.token)
            else -> error("Can't convert $booleanConstantDescriptor to Analysis API")
        }

    override fun visitVariableReference(variableReference: VariableReference, data: Unit): Any =
        visitVariableReference(variableReference, ::KaBaseContractParameterValue)

    override fun visitBooleanVariableReference(
        booleanVariableReference: BooleanVariableReference,
        data: Unit
    ): Any = visitVariableReference(booleanVariableReference, ::KaBaseContractBooleanValueParameterExpression)

    private fun <T> visitVariableReference(
        variableReference: VariableReference,
        constructor: (KaParameterSymbol) -> T
    ): T = constructor(variableReference.descriptor.toKtSymbol(analysisContext) as KaParameterSymbol)

    // Util function to avoid hard coding names of the classes. Type inference will do a better job figuring out the best type to cast to.
    // This visitor isn't type-safe anyway
    private inline fun <reified T> ContractDescriptionElement.accept() = accept(this@ContractDescriptionElementToAnalysisApi, Unit) as T
}
