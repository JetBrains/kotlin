/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.contracts.description.KtAbstractConstantReference.KtBooleanConstantReference
import org.jetbrains.kotlin.analysis.api.contracts.description.KtAbstractValueParameterReference.KtBooleanValueParameterReference
import org.jetbrains.kotlin.analysis.api.contracts.description.KtAbstractValueParameterReference.KtValueParameterReference
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.contracts.description.expressions.*
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor

internal fun EffectDeclaration.effectDeclarationToAnalysisApi(analysisContext: Fe10AnalysisContext): KtEffectDeclaration =
    accept(ContractDescriptionElementToAnalysisApi(analysisContext), Unit).cast()

private class ContractDescriptionElementToAnalysisApi(val analysisContext: Fe10AnalysisContext) :
    ContractDescriptionVisitor<KtContractDescriptionElement, Unit> {

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConditionalEffectDeclaration,
        data: Unit
    ): KtContractDescriptionElement = KtConditionalEffectDeclaration(
        conditionalEffect.effect.accept(this, data).cast(),
        conditionalEffect.condition.accept(this, data).cast(),
    )

    override fun visitReturnsEffectDeclaration(returnsEffect: ReturnsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtReturnsEffectDeclaration(returnsEffect.value.accept(this, data).cast())

    override fun visitCallsEffectDeclaration(callsEffect: CallsEffectDeclaration, data: Unit): KtContractDescriptionElement =
        KtCallsEffectDeclaration(callsEffect.variableReference.accept(this, data).cast(), callsEffect.kind)

    override fun visitLogicalOr(logicalOr: LogicalOr, data: Unit): KtContractDescriptionElement = KtBinaryLogicExpression(
        logicalOr.left.accept(this, data).cast(),
        logicalOr.right.accept(this, data).cast(),
        KtBinaryLogicExpression.KtLogicOperationKind.OR
    )

    override fun visitLogicalAnd(logicalAnd: LogicalAnd, data: Unit): KtContractDescriptionElement = KtBinaryLogicExpression(
        logicalAnd.left.accept(this, data).cast(),
        logicalAnd.right.accept(this, data).cast(),
        KtBinaryLogicExpression.KtLogicOperationKind.AND
    )

    override fun visitLogicalNot(logicalNot: LogicalNot, data: Unit): KtContractDescriptionElement =
        KtLogicalNot(logicalNot.arg.accept(this, data).cast())

    override fun visitIsInstancePredicate(isInstancePredicate: IsInstancePredicate, data: Unit): KtContractDescriptionElement =
        KtIsInstancePredicate(
            isInstancePredicate.arg.accept(this, data).cast(),
            isInstancePredicate.type.toKtType(analysisContext),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: IsNullPredicate, data: Unit): KtContractDescriptionElement =
        KtIsNullPredicate(isNullPredicate.arg.accept(this, data).cast(), isNullPredicate.isNegated)

    override fun visitConstantDescriptor(constantReference: ConstantReference, data: Unit): KtContractDescriptionElement =
        KtAbstractConstantReference.KtConstantReference(constantReference.name, analysisContext.token)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: BooleanConstantReference,
        data: Unit
    ): KtContractDescriptionElement =
        when (booleanConstantDescriptor) {
            BooleanConstantReference.TRUE -> KtBooleanConstantReference.KtTrue(analysisContext.token)
            BooleanConstantReference.FALSE -> KtBooleanConstantReference.KtFalse(analysisContext.token)
            else -> error("Can't convert $booleanConstantDescriptor to Analysis API")
        }

    override fun visitVariableReference(variableReference: VariableReference, data: Unit): KtContractDescriptionElement =
        visitVariableReference(variableReference, ::KtValueParameterReference)

    override fun visitBooleanVariableReference(
        booleanVariableReference: BooleanVariableReference,
        data: Unit
    ): KtContractDescriptionElement = visitVariableReference(booleanVariableReference, ::KtBooleanValueParameterReference)

    private fun visitVariableReference(
        variableReference: VariableReference,
        constructor: (Int, String, KtLifetimeToken) -> KtAbstractValueParameterReference
    ): KtAbstractValueParameterReference = constructor(
        when (val descriptor = variableReference.descriptor) {
            is ValueParameterDescriptor -> descriptor.index
            is ReceiverParameterDescriptor -> -1
            else -> error("Can't find $variableReference index")
        },
        variableReference.descriptor.name.asStringStripSpecialMarkers(),
        analysisContext.token
    )
}

// Util function to avoid hard coding names of the classes. Type inference will do a better job figuring out the best type to cast to.
// This visitor isn't type-safe anyway
private inline fun <reified T : KtContractDescriptionElement> Any.cast() = this as T
