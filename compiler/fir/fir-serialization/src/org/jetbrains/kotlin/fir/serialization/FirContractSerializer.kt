/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.KtContractDescriptionVisitor
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags

class FirContractSerializer {
    fun serializeContractOfFunctionIfAny(
        function: FirFunction,
        proto: ProtoBuf.Function.Builder,
        parentSerializer: FirElementSerializer
    ) {
        val contractDescription = (function as? FirSimpleFunction)?.contractDescription
        if (contractDescription == null || contractDescription.effects.isNullOrEmpty()) {
            return
        }
        val worker = ContractSerializerWorker(parentSerializer)
        proto.setContract(worker.contractProto(contractDescription))
    }

    private class ContractSerializerWorker(private val parentSerializer: FirElementSerializer) {
        fun contractProto(contractDescription: FirContractDescription): ProtoBuf.Contract.Builder {
            return ProtoBuf.Contract.newBuilder().apply {
                contractDescription.effects?.forEach { addEffect(effectProto(it.effect, contractDescription)) }
            }
        }

        private fun effectProto(
            effectDeclaration: ConeEffectDeclaration, contractDescription: FirContractDescription
        ): ProtoBuf.Effect.Builder {
            return ProtoBuf.Effect.newBuilder().apply {
                fillEffectProto(this, effectDeclaration, contractDescription)
            }
        }

        private fun fillEffectProto(
            builder: ProtoBuf.Effect.Builder,
            effectDeclaration: ConeEffectDeclaration,
            contractDescription: FirContractDescription
        ) {
            when (effectDeclaration) {
                is ConeConditionalEffectDeclaration -> {
                    builder.setConclusionOfConditionalEffect(contractExpressionProto(effectDeclaration.condition, contractDescription))
                    fillEffectProto(builder, effectDeclaration.effect, contractDescription)
                }

                is ConeReturnsEffectDeclaration -> {
                    when (effectDeclaration.value) {
                        ConeContractConstantValues.NOT_NULL ->
                            builder.effectType = ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL
                        ConeContractConstantValues.WILDCARD ->
                            builder.effectType = ProtoBuf.Effect.EffectType.RETURNS_CONSTANT
                        else -> {
                            builder.effectType = ProtoBuf.Effect.EffectType.RETURNS_CONSTANT
                            builder.addEffectConstructorArgument(contractExpressionProto(effectDeclaration.value, contractDescription))
                        }
                    }
                }

                is ConeCallsEffectDeclaration -> {
                    builder.effectType = ProtoBuf.Effect.EffectType.CALLS
                    builder.addEffectConstructorArgument(
                        contractExpressionProto(effectDeclaration.valueParameterReference, contractDescription)
                    )
                    val invocationKindProtobufEnum = invocationKindProtobufEnum(effectDeclaration.kind)
                    if (invocationKindProtobufEnum != null) {
                        builder.kind = invocationKindProtobufEnum
                    }
                }

                is ConeConditionalReturnsDeclaration -> {
                    builder.conditionKind = ProtoBuf.Effect.EffectConditionKind.RETURNS_CONDITION
                    builder.setConclusionOfConditionalEffect(contractExpressionProto(effectDeclaration.argumentsCondition, contractDescription))
                    fillEffectProto(builder, effectDeclaration.returnsEffect, contractDescription)
                }
                else -> {
                    throw IllegalStateException("Unsupported effect type: ${effectDeclaration::class.simpleName}")
                }
            }
        }

        private fun contractExpressionProto(
            contractDescriptionElement: ConeContractDescriptionElement,
            contractDescription: FirContractDescription
        ): ProtoBuf.Expression.Builder {
            return contractDescriptionElement.accept(object : KtContractDescriptionVisitor<ProtoBuf.Expression.Builder, Unit, ConeKotlinType, ConeDiagnostic>() {
                override fun visitLogicalBinaryOperationContractExpression(
                    binaryLogicExpression: ConeBinaryLogicExpression,
                    data: Unit
                ): ProtoBuf.Expression.Builder {
                    return if (binaryLogicExpression.kind == LogicOperationKind.AND) {
                        visitLogicalAnd(binaryLogicExpression, data)
                    } else {
                        visitLogicalOr(binaryLogicExpression, data)
                    }
                }

                private fun visitLogicalOr(logicalOr: ConeBinaryLogicExpression, data: Unit): ProtoBuf.Expression.Builder {
                    val leftBuilder = logicalOr.left.accept(this, data)

                    return if (leftBuilder.andArgumentCount != 0) {
                        // can't flatten and re-use left builder
                        ProtoBuf.Expression.newBuilder().apply {
                            addOrArgument(leftBuilder)
                            addOrArgument(contractExpressionProto(logicalOr.right, contractDescription))
                        }
                    } else {
                        // we can save some space by re-using left builder instead of nesting new one
                        leftBuilder.apply { addOrArgument(contractExpressionProto(logicalOr.right, contractDescription)) }
                    }
                }

                private fun visitLogicalAnd(logicalAnd: ConeBinaryLogicExpression, data: Unit): ProtoBuf.Expression.Builder {
                    val leftBuilder = logicalAnd.left.accept(this, data)

                    return if (leftBuilder.orArgumentCount != 0) {
                        // leftBuilder is already a sequence of Or-operators, so we can't re-use it
                        ProtoBuf.Expression.newBuilder().apply {
                            addAndArgument(leftBuilder)
                            addAndArgument(contractExpressionProto(logicalAnd.right, contractDescription))
                        }
                    } else {
                        // we can save some space by re-using left builder instead of nesting new one
                        leftBuilder.apply { addAndArgument(contractExpressionProto(logicalAnd.right, contractDescription)) }
                    }
                }

                override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Unit): ProtoBuf.Expression.Builder =
                    logicalNot.arg.accept(this, data).apply {
                        writeFlags(Flags.IS_NEGATED.invert(flags))
                    }

                override fun visitIsInstancePredicate(
                    isInstancePredicate: ConeIsInstancePredicate, data: Unit
                ): ProtoBuf.Expression.Builder {
                    // write variable
                    val builder = visitValueParameterReference(isInstancePredicate.arg, data)

                    // write rhs type
                    builder.isInstanceTypeId = parentSerializer.typeId(isInstancePredicate.type)

                    // set flags
                    builder.writeFlags(Flags.getContractExpressionFlags(isInstancePredicate.isNegated, false))

                    return builder
                }

                override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Unit): ProtoBuf.Expression.Builder {
                    // get builder with variable embedded into it
                    val builder = visitValueParameterReference(isNullPredicate.arg, data)

                    // set flags
                    builder.writeFlags(Flags.getContractExpressionFlags(isNullPredicate.isNegated, true))

                    return builder
                }

                override fun visitConstantDescriptor(constantReference: ConeConstantReference, data: Unit): ProtoBuf.Expression.Builder {
                    val builder = ProtoBuf.Expression.newBuilder()

                    // write constant value
                    val constantValueProtobufEnum = constantValueProtobufEnum(constantReference)
                    if (constantValueProtobufEnum != null) {
                        builder.constantValue = constantValueProtobufEnum
                    }

                    return builder
                }

                override fun visitValueParameterReference(
                    valueParameterReference: ConeValueParameterReference, data: Unit
                ): ProtoBuf.Expression.Builder {
                    val builder = ProtoBuf.Expression.newBuilder()

                    val indexOfParameter = valueParameterReference.parameterIndex + 1
                    builder.valueParameterReference = indexOfParameter

                    return builder
                }
            }, Unit)
        }

        private fun ProtoBuf.Expression.Builder.writeFlags(newFlagsValue: Int) {
            if (flags != newFlagsValue) {
                flags = newFlagsValue
            }
        }

        private fun invocationKindProtobufEnum(kind: EventOccurrencesRange): ProtoBuf.Effect.InvocationKind? = when (kind) {
            EventOccurrencesRange.AT_MOST_ONCE -> ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE
            EventOccurrencesRange.EXACTLY_ONCE -> ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE
            EventOccurrencesRange.AT_LEAST_ONCE -> ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE
            else -> null
        }

        private fun constantValueProtobufEnum(constantReference: ConeConstantReference): ProtoBuf.Expression.ConstantValue? =
            when (constantReference) {
                ConeContractConstantValues.TRUE -> ProtoBuf.Expression.ConstantValue.TRUE
                ConeContractConstantValues.FALSE -> ProtoBuf.Expression.ConstantValue.FALSE
                ConeContractConstantValues.NULL -> ProtoBuf.Expression.ConstantValue.NULL
                ConeContractConstantValues.NOT_NULL -> throw IllegalStateException(
                    "Internal error during serialization of function contract: NOT_NULL constant isn't denotable in protobuf format. " +
                            "Its serialization should be handled at higher level"
                )
                ConeContractConstantValues.WILDCARD -> null
                else -> throw IllegalArgumentException("Unknown constant: $constantReference")
            }
    }
}
