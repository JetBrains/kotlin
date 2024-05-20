/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConstantValue.KaContractConstantType
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractReturnsContractEffectDeclaration.*
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal fun KtEffectDeclaration<ConeKotlinType, ConeDiagnostic>.coneEffectDeclarationToAnalysisApi(
    builder: KaSymbolByFirBuilder,
    firFunctionSymbol: KaFirFunctionSymbol
): KaContractEffectDeclaration =
    accept(ConeContractDescriptionElementToAnalysisApi(builder, firFunctionSymbol), Unit) as KaContractEffectDeclaration

private class ConeContractDescriptionElementToAnalysisApi(
    private val builder: KaSymbolByFirBuilder,
    private val firFunctionSymbol: KaFirFunctionSymbol
) : KtContractDescriptionVisitor<Any, Unit, ConeKotlinType, ConeDiagnostic>() {

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConeConditionalEffectDeclaration,
        data: Unit
    ): KaContractConditionalContractEffectDeclaration = KaContractConditionalContractEffectDeclaration(
        conditionalEffect.effect.accept(),
        conditionalEffect.condition.accept()
    )

    override fun visitReturnsEffectDeclaration(
        returnsEffect: ConeReturnsEffectDeclaration,
        data: Unit
    ): KaContractReturnsContractEffectDeclaration =
        when (val value = returnsEffect.value) {
            ConeContractConstantValues.NULL ->
                KaContractReturnsSpecificValueEffectDeclaration(KaContractConstantValue(KaContractConstantType.NULL, builder.token))
            ConeContractConstantValues.NOT_NULL -> KaContractReturnsNotNullEffectDeclaration(builder.token)
            ConeContractConstantValues.WILDCARD -> KaContractReturnsSuccessfullyEffectDeclaration(builder.token)
            is KtBooleanConstantReference -> KaContractReturnsSpecificValueEffectDeclaration(
                KaContractConstantValue(
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
        KaContractCallsInPlaceContractEffectDeclaration(
            callsEffect.valueParameterReference.accept(),
            callsEffect.kind
        )

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: ConeBinaryLogicExpression,
        data: Unit
    ): KaContractBinaryLogicExpression = KaContractBinaryLogicExpression(
        binaryLogicExpression.left.accept(),
        binaryLogicExpression.right.accept(),
        when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> KaContractBinaryLogicExpression.KaLogicOperation.AND
            LogicOperationKind.OR -> KaContractBinaryLogicExpression.KaLogicOperation.OR
        }
    )

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Unit): KaContractLogicalNotExpression =
        KaContractLogicalNotExpression(logicalNot.arg.accept())

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Unit): KaContractIsInstancePredicateExpression =
        KaContractIsInstancePredicateExpression(
            isInstancePredicate.arg.accept(),
            builder.typeBuilder.buildKtType(isInstancePredicate.type),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Unit): KaContractIsNullPredicateExpression =
        KaContractIsNullPredicateExpression(isNullPredicate.arg.accept(), isNullPredicate.isNegated)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: ConeBooleanConstantReference,
        data: Unit
    ): KaContractBooleanConstantExpression =
        when (booleanConstantDescriptor) {
            ConeContractConstantValues.TRUE -> KaContractBooleanConstantExpression(true, builder.token)
            ConeContractConstantValues.FALSE -> KaContractBooleanConstantExpression(false, builder.token)
            else -> error("Can't convert $booleanConstantDescriptor to Analysis API")
        }

    override fun visitValueParameterReference(
        valueParameterReference: ConeValueParameterReference,
        data: Unit
    ): Any = visitValueParameterReference(valueParameterReference, ::KaContractParameterValue)

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: ConeBooleanValueParameterReference,
        data: Unit
    ): Any =
        visitValueParameterReference(booleanValueParameterReference, ::KaContractBooleanValueParameterExpression)

    private fun <T> visitValueParameterReference(
        valueParameterReference: ConeValueParameterReference,
        constructor: (KaParameterSymbol) -> T
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
