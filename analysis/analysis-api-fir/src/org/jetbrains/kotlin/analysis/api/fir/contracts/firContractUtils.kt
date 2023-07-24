/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.contracts.description.KtContractConstantValue.KtContractConstantType
import org.jetbrains.kotlin.analysis.api.contracts.description.KtContractReturnsContractEffectDeclaration.*
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtParameterSymbol
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal fun KtEffectDeclaration<ConeKotlinType, ConeDiagnostic>.coneEffectDeclarationToAnalysisApi(
    builder: KtSymbolByFirBuilder,
    firFunctionSymbol: KtFirFunctionSymbol
): KtContractEffectDeclaration =
    accept(ConeContractDescriptionElementToAnalysisApi(builder, firFunctionSymbol), Unit) as KtContractEffectDeclaration

private class ConeContractDescriptionElementToAnalysisApi(
    private val builder: KtSymbolByFirBuilder,
    private val firFunctionSymbol: KtFirFunctionSymbol
) : KtContractDescriptionVisitor<Any, Unit, ConeKotlinType, ConeDiagnostic>() {

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConeConditionalEffectDeclaration,
        data: Unit
    ): KtContractConditionalContractEffectDeclaration = KtContractConditionalContractEffectDeclaration(
        conditionalEffect.effect.accept(),
        conditionalEffect.condition.accept()
    )

    override fun visitReturnsEffectDeclaration(
        returnsEffect: ConeReturnsEffectDeclaration,
        data: Unit
    ): KtContractReturnsContractEffectDeclaration =
        when (val value = returnsEffect.value) {
            ConeContractConstantValues.NULL ->
                KtContractReturnsSpecificValueEffectDeclaration(KtContractConstantValue(KtContractConstantType.NULL, builder.token))
            ConeContractConstantValues.NOT_NULL -> KtContractReturnsNotNullEffectDeclaration(builder.token)
            ConeContractConstantValues.WILDCARD -> KtContractReturnsSuccessfullyEffectDeclaration(builder.token)
            is KtBooleanConstantReference -> KtContractReturnsSpecificValueEffectDeclaration(
                KtContractConstantValue(
                    when (value) {
                        ConeContractConstantValues.TRUE -> KtContractConstantType.TRUE
                        ConeContractConstantValues.FALSE -> KtContractConstantType.FALSE
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

    override fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration<ConeKotlinType, ConeDiagnostic>, data: Unit): KtContractCallsInPlaceContractEffectDeclaration =
        KtContractCallsInPlaceContractEffectDeclaration(
            callsEffect.valueParameterReference.accept(),
            callsEffect.kind
        )

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: ConeBinaryLogicExpression,
        data: Unit
    ): KtContractBinaryLogicExpression = KtContractBinaryLogicExpression(
        binaryLogicExpression.left.accept(),
        binaryLogicExpression.right.accept(),
        when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> KtContractBinaryLogicExpression.KtLogicOperation.AND
            LogicOperationKind.OR -> KtContractBinaryLogicExpression.KtLogicOperation.OR
        }
    )

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Unit): KtContractLogicalNotExpression =
        KtContractLogicalNotExpression(logicalNot.arg.accept())

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Unit): KtContractIsInstancePredicateExpression =
        KtContractIsInstancePredicateExpression(
            isInstancePredicate.arg.accept(),
            builder.typeBuilder.buildKtType(isInstancePredicate.type),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Unit): KtContractIsNullPredicateExpression =
        KtContractIsNullPredicateExpression(isNullPredicate.arg.accept(), isNullPredicate.isNegated)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: ConeBooleanConstantReference,
        data: Unit
    ): KtContractBooleanConstantExpression =
        when (booleanConstantDescriptor) {
            ConeContractConstantValues.TRUE -> KtContractBooleanConstantExpression(true, builder.token)
            ConeContractConstantValues.FALSE -> KtContractBooleanConstantExpression(false, builder.token)
            else -> error("Can't convert $booleanConstantDescriptor to Analysis API")
        }

    override fun visitValueParameterReference(
        valueParameterReference: ConeValueParameterReference,
        data: Unit
    ): Any = visitValueParameterReference(valueParameterReference, ::KtContractParameterValue)

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: ConeBooleanValueParameterReference,
        data: Unit
    ): Any =
        visitValueParameterReference(booleanValueParameterReference, ::KtContractBooleanValueParameterExpression)

    private fun <T> visitValueParameterReference(
        valueParameterReference: ConeValueParameterReference,
        constructor: (KtParameterSymbol) -> T
    ): T = constructor(
        if (valueParameterReference.parameterIndex == -1) firFunctionSymbol.receiverParameter
            ?: errorWithAttachment("${firFunctionSymbol::class} should contain a receiver") {
                withFirEntry("fir", firFunctionSymbol.firSymbol.fir)
            }
        else firFunctionSymbol.valueParameters[valueParameterReference.parameterIndex]
    )

    // Util function to avoid hard coding names of the classes. Type inference will do a better job figuring out the best type to cast to.
    // This visitor isn't type-safe anyway
    private inline fun <reified T> ConeContractDescriptionElement.accept() =
        accept(this@ConeContractDescriptionElementToAnalysisApi, Unit) as T
}
