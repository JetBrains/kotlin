/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.builder.buildResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContractConstantValues
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyAccessorStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeBean
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class StubBasedFirContractDeserializer(
    private val contractOwner: FirContractDescriptionOwner,
    private val typeDeserializer: StubBasedFirTypeDeserializer,
) {
    fun loadContract(declaration: KtDeclarationWithBody): FirContractDescription? {
        val contract = when (declaration) {
            is KtNamedFunction -> {
                val functionStub: KotlinFunctionStubImpl = declaration.compiledStub
                functionStub.contract
            }

            is KtPropertyAccessor -> {
                val accessorStub: KotlinPropertyAccessorStubImpl = declaration.compiledStub
                accessorStub.contract
            }

            else -> errorWithAttachment("Unsupported declaration ${declaration::class.simpleName}") {
                withPsiEntry("declaration", declaration)
            }
        }

        val effects = contract?.map {
            it.accept(ContractDescriptionConvertingVisitor(), null)
        }

        if (effects.isNullOrEmpty()) return null
        return buildResolvedContractDescription {
            this.effects += effects.map { description ->
                buildEffectDeclaration {
                    effect = description as ConeEffectDeclaration
                }
            }
        }
    }

    inner class ContractDescriptionConvertingVisitor :
        KtContractDescriptionVisitor<ConeContractDescriptionElement, Nothing?, KotlinTypeBean, Nothing?>() {
        override fun visitCallsEffectDeclaration(
            callsEffect: KtCallsEffectDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            return ConeCallsEffectDeclaration(
                callsEffect.valueParameterReference.accept(
                    this,
                    data
                ) as KtValueParameterReference<ConeKotlinType, ConeDiagnostic>, callsEffect.kind
            )
        }

        override fun visitReturnsEffectDeclaration(
            returnsEffect: KtReturnsEffectDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            return ConeReturnsEffectDeclaration(
                returnsEffect.value.accept(
                    this,
                    data
                ) as KtConstantReference<ConeKotlinType, ConeDiagnostic>
            )
        }

        override fun visitConditionalEffectDeclaration(
            conditionalEffect: KtConditionalEffectDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            return ConeConditionalEffectDeclaration(
                conditionalEffect.effect.accept(this, data) as KtEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
                conditionalEffect.condition.accept(this, data) as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>
            )
        }

        override fun visitConditionalReturnsDeclaration(
            conditionalEffect: KtConditionalReturnsDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?,
        ): ConeContractDescriptionElement {
            return ConeConditionalReturnsDeclaration(
                conditionalEffect.argumentsCondition.accept(this, data) as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>,
                conditionalEffect.returnsEffect.accept(this, data) as KtEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
            )
        }

        override fun visitHoldsInEffectDeclaration(
            holdsInEffect: KtHoldsInEffectDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?,
        ): ConeContractDescriptionElement {
            return ConeHoldsInEffectDeclaration(
                holdsInEffect.argumentsCondition.accept(this, data) as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>,
                holdsInEffect.valueParameterReference.accept(this, data) as KtValueParameterReference<ConeKotlinType, ConeDiagnostic>
            )
        }

        override fun visitLogicalBinaryOperationContractExpression(
            binaryLogicExpression: KtBinaryLogicExpression<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            return ConeBinaryLogicExpression(
                binaryLogicExpression.left.accept(this, data) as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>,
                binaryLogicExpression.right.accept(this, data) as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>,
                binaryLogicExpression.kind
            )
        }

        override fun visitLogicalNot(
            logicalNot: KtLogicalNot<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            return ConeLogicalNot(logicalNot.arg.accept(this, data) as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>)
        }

        override fun visitIsInstancePredicate(
            isInstancePredicate: KtIsInstancePredicate<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            return ConeIsInstancePredicate(
                isInstancePredicate.arg.accept(this, data) as KtValueParameterReference<ConeKotlinType, ConeDiagnostic>,
                typeDeserializer.type(isInstancePredicate.type)!!,
                isInstancePredicate.isNegated
            )
        }

        override fun visitIsNullPredicate(
            isNullPredicate: KtIsNullPredicate<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            return ConeIsNullPredicate(
                isNullPredicate.arg.accept(this, data) as KtValueParameterReference<ConeKotlinType, ConeDiagnostic>,
                isNullPredicate.isNegated
            )
        }

        override fun visitValueParameterReference(
            valueParameterReference: KtValueParameterReference<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            val parameterIndex = valueParameterReference.parameterIndex
            return ConeValueParameterReference(parameterIndex, getParameterName(parameterIndex))
        }

        override fun visitBooleanValueParameterReference(
            booleanValueParameterReference: KtBooleanValueParameterReference<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            return ConeBooleanValueParameterReference(
                booleanValueParameterReference.parameterIndex,
                getParameterName(booleanValueParameterReference.parameterIndex)
            )
        }

        private fun getParameterName(parameterIndex: Int): String = when {
            parameterIndex == -1 -> "this"
            parameterIndex < contractOwner.valueParameters.size -> contractOwner.valueParameters[parameterIndex].name.asString()
            else -> {
                val contextParameters = when (contractOwner) {
                    is FirNamedFunction -> contractOwner.contextParameters
                    is FirPropertyAccessor -> contractOwner.propertySymbol.fir.contextParameters
                    else -> errorWithAttachment("Unsupported contract owner kind: ${contractOwner::class.simpleName}"){
                        withFirEntry("contractOwner", contractOwner)
                    }
                }

                val contextParameterIndex = parameterIndex - contractOwner.valueParameters.size
                contextParameters[contextParameterIndex].name.asString()
            }
        }

        override fun visitConstantDescriptor(
            constantReference: KtConstantReference<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement {
            return when (constantReference) {
                KotlinContractConstantValues.FALSE -> ConeContractConstantValues.FALSE
                KotlinContractConstantValues.TRUE -> ConeContractConstantValues.TRUE
                KotlinContractConstantValues.NULL -> ConeContractConstantValues.NULL
                KotlinContractConstantValues.NOT_NULL -> ConeContractConstantValues.NOT_NULL
                KotlinContractConstantValues.WILDCARD -> ConeContractConstantValues.WILDCARD
                else -> {
                    error("Unexpected constant: $constantReference")
                }
            }
        }
    }
}
