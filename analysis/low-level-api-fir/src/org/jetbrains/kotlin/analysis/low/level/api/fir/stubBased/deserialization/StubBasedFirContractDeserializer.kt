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
        val effectDeclarations = when (declaration) {
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

        val effects = effectDeclarations?.mapNotNull {
            it.accept(ContractDescriptionConvertingVisitor(), data = null)
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
        KtContractDescriptionVisitor<ConeContractDescriptionElement?, Nothing?, KotlinTypeBean, Nothing?>() {
        override fun visitContractDescriptionElement(
            contractDescriptionElement: KtContractDescriptionElement<KotlinTypeBean, Nothing?>,
            data: Nothing?,
        ): ConeContractDescriptionElement? {
            return null
        }

        override fun visitCallsEffectDeclaration(
            callsEffect: KtCallsEffectDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement? {
            val parameterReference = callsEffect.valueParameterReference.accept(this, data) ?: return null
            return ConeCallsEffectDeclaration(
                parameterReference as KtValueParameterReference<ConeKotlinType, ConeDiagnostic>,
                callsEffect.kind
            )
        }

        override fun visitReturnsResultOfEffectDeclaration(
            returnsResultOfEffect: KtReturnsResultOfDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement? {
            val parameterReference = returnsResultOfEffect.valueParameterReference.accept(this, data) ?: return null
            return ConeReturnsResultOfDeclaration(
                parameterReference as KtValueParameterReference<ConeKotlinType, ConeDiagnostic>
            )
        }

        override fun visitReturnsEffectDeclaration(
            returnsEffect: KtReturnsEffectDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement? {
            val value = returnsEffect.value.accept(this, data) ?: return null
            return ConeReturnsEffectDeclaration(
                value as KtConstantReference<ConeKotlinType, ConeDiagnostic>
            )
        }

        override fun visitConditionalEffectDeclaration(
            conditionalEffect: KtConditionalEffectDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement? {
            val effect = conditionalEffect.effect.accept(this, data) ?: return null
            val condition = conditionalEffect.condition.accept(this, data) ?: return null
            return ConeConditionalEffectDeclaration(
                effect as KtEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
                condition as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>
            )
        }

        override fun visitConditionalReturnsDeclaration(
            conditionalEffect: KtConditionalReturnsDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?,
        ): ConeContractDescriptionElement? {
            val condition = conditionalEffect.argumentsCondition.accept(this, data) ?: return null
            val effect = conditionalEffect.returnsEffect.accept(this, data) ?: return null
            return ConeConditionalReturnsDeclaration(
                condition as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>,
                effect as KtEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
            )
        }

        override fun visitHoldsInEffectDeclaration(
            holdsInEffect: KtHoldsInEffectDeclaration<KotlinTypeBean, Nothing?>,
            data: Nothing?,
        ): ConeContractDescriptionElement? {
            val condition = holdsInEffect.argumentsCondition.accept(this, data) ?: return null
            val parameterReference = holdsInEffect.valueParameterReference.accept(this, data) ?: return null
            return ConeHoldsInEffectDeclaration(
                condition as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>,
                parameterReference as KtValueParameterReference<ConeKotlinType, ConeDiagnostic>
            )
        }

        override fun visitLogicalBinaryOperationContractExpression(
            binaryLogicExpression: KtBinaryLogicExpression<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement? {
            val left = binaryLogicExpression.left.accept(this, data) ?: return null
            val right = binaryLogicExpression.right.accept(this, data) ?: return null
            return ConeBinaryLogicExpression(
                left as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>,
                right as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>,
                binaryLogicExpression.kind
            )
        }

        override fun visitLogicalNot(
            logicalNot: KtLogicalNot<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement? {
            val arg = logicalNot.arg.accept(this, data) ?: return null
            return ConeLogicalNot(arg as KtBooleanExpression<ConeKotlinType, ConeDiagnostic>)
        }

        override fun visitIsInstancePredicate(
            isInstancePredicate: KtIsInstancePredicate<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement? {
            val arg = isInstancePredicate.arg.accept(this, data) ?: return null
            val type = typeDeserializer.type(isInstancePredicate.type) ?: return null
            return ConeIsInstancePredicate(
                arg as KtValueParameterReference<ConeKotlinType, ConeDiagnostic>,
                type,
                isInstancePredicate.isNegated
            )
        }

        override fun visitIsNullPredicate(
            isNullPredicate: KtIsNullPredicate<KotlinTypeBean, Nothing?>,
            data: Nothing?
        ): ConeContractDescriptionElement? {
            val arg = isNullPredicate.arg.accept(this, data) ?: return null
            return ConeIsNullPredicate(
                arg as KtValueParameterReference<ConeKotlinType, ConeDiagnostic>,
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
        ): ConeContractDescriptionElement? {
            return when (constantReference) {
                KotlinContractConstantValues.FALSE -> ConeContractConstantValues.FALSE
                KotlinContractConstantValues.TRUE -> ConeContractConstantValues.TRUE
                KotlinContractConstantValues.NULL -> ConeContractConstantValues.NULL
                KotlinContractConstantValues.NOT_NULL -> ConeContractConstantValues.NOT_NULL
                KotlinContractConstantValues.WILDCARD -> ConeContractConstantValues.WILDCARD
                else -> null
            }
        }
    }
}
