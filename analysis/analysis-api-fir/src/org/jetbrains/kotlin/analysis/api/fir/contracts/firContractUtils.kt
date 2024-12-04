/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConstantValue.KaContractConstantType
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedFunctionSymbol
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
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

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
    ): KaContractConditionalContractEffectDeclaration = KaBaseContractConditionalContractEffectDeclaration(
        conditionalEffect.effect.accept(),
        conditionalEffect.condition.accept()
    )

    override fun visitReturnsEffectDeclaration(
        returnsEffect: ConeReturnsEffectDeclaration,
        data: Unit
    ): KaContractReturnsContractEffectDeclaration =
        when (val value = returnsEffect.value) {
            ConeContractConstantValues.NULL ->
                KaBaseContractReturnsSpecificValueEffectDeclaration(KaBaseContractConstantValue(KaContractConstantType.NULL, builder.token))
            ConeContractConstantValues.NOT_NULL -> KaBaseContractReturnsNotNullEffectDeclaration(builder.token)
            ConeContractConstantValues.WILDCARD -> KaBaseContractReturnsSuccessfullyEffectDeclaration(builder.token)
            is KtBooleanConstantReference -> KaBaseContractReturnsSpecificValueEffectDeclaration(
                KaBaseContractConstantValue(
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
        KaBaseContractCallsInPlaceContractEffectDeclaration(
            callsEffect.valueParameterReference.accept(),
            callsEffect.kind,
        )

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: ConeBinaryLogicExpression,
        data: Unit
    ): KaContractBinaryLogicExpression = KaBaseContractBinaryLogicExpression(
        binaryLogicExpression.left.accept(),
        binaryLogicExpression.right.accept(),
        when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> KaContractBinaryLogicExpression.KaLogicOperation.AND
            LogicOperationKind.OR -> KaContractBinaryLogicExpression.KaLogicOperation.OR
        }
    )

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Unit): KaContractLogicalNotExpression =
        KaBaseContractLogicalNotExpression(logicalNot.arg.accept())

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Unit): KaContractIsInstancePredicateExpression =
        KaBaseContractIsInstancePredicateExpression(
            isInstancePredicate.arg.accept(),
            builder.typeBuilder.buildKtType(isInstancePredicate.type),
            isInstancePredicate.isNegated
        )

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Unit): KaContractIsNullPredicateExpression =
        KaBaseContractIsNullPredicateExpression(isNullPredicate.arg.accept(), isNullPredicate.isNegated)

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: ConeBooleanConstantReference,
        data: Unit
    ): KaContractBooleanConstantExpression =
        when (booleanConstantDescriptor) {
            ConeContractConstantValues.TRUE -> KaBaseContractBooleanConstantExpression(true, builder.token)
            ConeContractConstantValues.FALSE -> KaBaseContractBooleanConstantExpression(false, builder.token)
            else -> error("Can't convert $booleanConstantDescriptor to Analysis API")
        }

    override fun visitValueParameterReference(
        valueParameterReference: ConeValueParameterReference,
        data: Unit
    ): Any = visitValueParameterReference(valueParameterReference, ::KaBaseContractParameterValue)

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: ConeBooleanValueParameterReference,
        data: Unit
    ): Any =
        visitValueParameterReference(booleanValueParameterReference, ::KaBaseContractBooleanValueParameterExpression)

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
