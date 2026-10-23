/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConstantValue.KaContractConstantType
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.contracts.description.*
import org.jetbrains.kotlin.analysis.api.fir.contracts.description.KaFirContractReturnsContractEffectDeclarations.KaFirContractReturnsNotNullEffectDeclaration
import org.jetbrains.kotlin.analysis.api.fir.contracts.description.KaFirContractReturnsContractEffectDeclarations.KaFirContractReturnsSpecificValueEffectDeclaration
import org.jetbrains.kotlin.analysis.api.fir.contracts.description.KaFirContractReturnsContractEffectDeclarations.KaFirContractReturnsSuccessfullyEffectDeclaration
import org.jetbrains.kotlin.analysis.api.fir.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

internal fun KtEffectDeclaration<ConeKotlinType, ConeDiagnostic>.coneEffectDeclarationToAnalysisApi(
    builder: KaSymbolByFirBuilder,
    firFunctionSymbol: KaFirNamedFunctionSymbol
): KaContractEffectDeclaration =
    accept(ConeContractDescriptionElementToAnalysisApi(builder, firFunctionSymbol), Unit) as KaContractEffectDeclaration

private class ConeContractDescriptionElementToAnalysisApi(
    private val builder: KaSymbolByFirBuilder,
    private val firFunctionSymbol: KaFirNamedFunctionSymbol
) : KtContractDescriptionVisitor<Any, Unit, ConeKotlinType, ConeDiagnostic>() {

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConeConditionalEffectDeclaration,
        data: Unit
    ): KaContractConditionalContractEffectDeclaration = KaFirContractConditionalContractEffectDeclaration(
        conditionalEffect.effect.accept(),
        conditionalEffect.condition.accept()
    )

    override fun visitReturnsEffectDeclaration(
        returnsEffect: ConeReturnsEffectDeclaration,
        data: Unit
    ): KaContractReturnsContractEffectDeclaration =
        when (val value = returnsEffect.value) {
            ConeContractConstantValues.NULL ->
                KaFirContractReturnsSpecificValueEffectDeclaration(KaFirContractConstantValue(KaContractConstantType.NULL, builder.token))
            ConeContractConstantValues.NOT_NULL -> KaFirContractReturnsNotNullEffectDeclaration(builder.token)
            ConeContractConstantValues.WILDCARD -> KaFirContractReturnsSuccessfullyEffectDeclaration(builder.token)
            is KtBooleanConstantReference -> KaFirContractReturnsSpecificValueEffectDeclaration(
                KaFirContractConstantValue(
                    when (value) {
                        ConeContractConstantValues.TRUE -> KaContractConstantType.TRUE
                        ConeContractConstantValues.FALSE -> KaContractConstantType.FALSE
                        else -> errorWithAttachment("Can't convert ${value::class} to the Analysis API") {
                            withEntry("value", value) { value.toString() }
                        }
                    },
                    builder.token
                )
            )
            else -> errorWithAttachment("Can't convert ${returnsEffect::class} to the Analysis API")  {
                withEntry("value", value) { value.toString() }
            }
        }

    override fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration<ConeKotlinType, ConeDiagnostic>, data: Unit): KaContractCallsInPlaceContractEffectDeclaration =
        KaFirContractCallsInPlaceContractEffectDeclaration(
            backingValueParameterReference = callsEffect.valueParameterReference.accept(),
            backingOccurrencesRange = callsEffect.kind,
        )

    override fun visitReturnsResultOfEffectDeclaration(
        returnsResultOfEffect: KtReturnsResultOfDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: Unit,
    ): KaContractReturnsResultOfEffectDeclaration =
        KaFirContractReturnsResultOfEffectDeclaration(
            returnsResultOfEffect.valueParameterReference.accept(),
        )

    override fun visitReturnsParameterEffectDeclaration(
        returnsParameterEffect: KtReturnsParameterDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: Unit,
    ): KaContractReturnsParameterEffectDeclaration =
        KaFirContractReturnsParameterEffectDeclaration(
            returnsParameterEffect.valueParameterReference.accept(),
        )

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: ConeBinaryLogicExpression,
        data: Unit
    ): KaContractBinaryLogicExpression = KaFirContractBinaryLogicExpression(
        binaryLogicExpression.left.accept(),
        binaryLogicExpression.right.accept(),
        when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> KaContractBinaryLogicExpression.KaLogicOperation.AND
            LogicOperationKind.OR -> KaContractBinaryLogicExpression.KaLogicOperation.OR
        }
    )

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Unit): KaContractLogicalNotExpression =
        KaFirContractLogicalNotExpression(logicalNot.arg.accept())

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Unit): KaContractIsInstancePredicateExpression =
        KaFirContractIsInstancePredicateExpression(
            isInstancePredicate.arg.accept(),
            builder.typeBuilder.buildKtType(isInstancePredicate.type),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Unit): KaContractIsNullPredicateExpression =
        KaFirContractIsNullPredicateExpression(isNullPredicate.arg.accept(), isNullPredicate.isNegated)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: ConeBooleanConstantReference,
        data: Unit
    ): KaContractBooleanConstantExpression =
        when (booleanConstantDescriptor) {
            ConeContractConstantValues.TRUE -> KaFirContractBooleanConstantExpression(true, builder.token)
            ConeContractConstantValues.FALSE -> KaFirContractBooleanConstantExpression(false, builder.token)
            else -> error("Can't convert $booleanConstantDescriptor to Analysis API")
        }

    override fun visitHoldsInEffectDeclaration(
        holdsInEffect: KtHoldsInEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: Unit,
    ): KaContractHoldsInEffectDeclaration = KaFirContractHoldsInEffectDeclaration(
        backingCondition = holdsInEffect.argumentsCondition.accept(),
        backingValueParameterReference = holdsInEffect.valueParameterReference.accept(),
    )

    override fun visitConditionalReturnsDeclaration(
        conditionalEffect: KtConditionalReturnsDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: Unit
    ): KaContractConditionalContractEffectDeclaration = KaFirContractConditionalContractEffectDeclaration(
        backingEffect = conditionalEffect.returnsEffect.accept(),
        backingCondition = conditionalEffect.argumentsCondition.accept(),
    )

    override fun visitValueParameterReference(
        valueParameterReference: ConeValueParameterReference,
        data: Unit
    ): KaContractParameterValue = visitValueParameterReference(valueParameterReference)

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: ConeBooleanValueParameterReference,
        data: Unit
    ): KaContractBooleanValueParameterExpression {
        val parameterValue = visitValueParameterReference(booleanValueParameterReference)
        return KaFirContractBooleanValueParameterExpression(parameterValue)
    }

    private fun visitValueParameterReference(valueParameterReference: ConeValueParameterReference): KaContractParameterValue {
        val parameterSymbol = when (val index = valueParameterReference.parameterIndex) {
            -1 -> firFunctionSymbol.receiverParameter
                ?: with(firFunctionSymbol.analysisSession) {
                    val containingClass = firFunctionSymbol.containingDeclaration
                    requireWithAttachment(containingClass is KaClassSymbol, { "Unexpected containing class" }) {
                        if (containingClass != null) {
                            withSymbolAttachment("containingDeclaration", this@with, containingClass)
                        }

                        withSymbolAttachment("functionSymbol", this@with, firFunctionSymbol)
                    }

                    return KaFirContractOwnerParameterValue(containingClass)
                }

            in firFunctionSymbol.valueParameters.indices -> firFunctionSymbol.valueParameters[index]

            // TODO: KT-82177 Property accessors are not supported in the Analysis API Surface
            else -> firFunctionSymbol.contextParameters.elementAtOrNull(index - firFunctionSymbol.valueParameters.size)
                ?: errorWithAttachment("${firFunctionSymbol::class.simpleName} doesn't contain parameter or context parameter with index $index") {
                    withFirEntry("fir", firFunctionSymbol.firSymbol.fir)
                }
        }

        return KaFirContractExplicitParameterValue(parameterSymbol)
    }

    // Util function to avoid hard coding names of the classes. Type inference will do a better job figuring out the best type to cast to.
    // This visitor isn't type-safe anyway
    private inline fun <reified T> ConeContractDescriptionElement.accept() =
        accept(this@ConeContractDescriptionElementToAnalysisApi, Unit) as T
}
