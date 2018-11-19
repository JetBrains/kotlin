/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.contracts.description.expressions.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags

class ContractSerializer {
    fun serializeContractOfFunctionIfAny(
        functionDescriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        parentSerializer: DescriptorSerializer
    ) {
        val contractDescription = functionDescriptor.getUserData(ContractProviderKey)?.getContractDescription()
        if (contractDescription != null) {
            val worker = ContractSerializerWorker(parentSerializer)
            proto.setContract(worker.contractProto(contractDescription))
        }
    }

    private class ContractSerializerWorker(private val parentSerializer: DescriptorSerializer) {
        fun contractProto(contractDescription: ContractDescription): ProtoBuf.Contract.Builder {
            return ProtoBuf.Contract.newBuilder().apply {
                contractDescription.effects.forEach { addEffect(effectProto(it, contractDescription)) }
            }
        }

        private fun effectProto(effectDeclaration: EffectDeclaration, contractDescription: ContractDescription): ProtoBuf.Effect.Builder {
            return ProtoBuf.Effect.newBuilder().apply {
                fillEffectProto(this, effectDeclaration, contractDescription)
            }
        }

        private fun fillEffectProto(
            builder: ProtoBuf.Effect.Builder,
            effectDeclaration: EffectDeclaration,
            contractDescription: ContractDescription
        ) {
            when (effectDeclaration) {
                is ConditionalEffectDeclaration -> {
                    builder.setConclusionOfConditionalEffect(contractExpressionProto(effectDeclaration.condition, contractDescription))
                    fillEffectProto(builder, effectDeclaration.effect, contractDescription)
                }

                is ReturnsEffectDeclaration -> {
                    when {
                        effectDeclaration.value == ConstantReference.NOT_NULL -> builder.effectType =
                                ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL
                        effectDeclaration.value == ConstantReference.WILDCARD -> builder.effectType =
                                ProtoBuf.Effect.EffectType.RETURNS_CONSTANT
                        else -> {
                            builder.effectType = ProtoBuf.Effect.EffectType.RETURNS_CONSTANT
                            builder.addEffectConstructorArgument(contractExpressionProto(effectDeclaration.value, contractDescription))
                        }
                    }
                }

                is CallsEffectDeclaration -> {
                    builder.effectType = ProtoBuf.Effect.EffectType.CALLS
                    builder.addEffectConstructorArgument(contractExpressionProto(effectDeclaration.variableReference, contractDescription))
                    val invocationKindProtobufEnum = invocationKindProtobufEnum(effectDeclaration.kind)
                    if (invocationKindProtobufEnum != null) {
                        builder.kind = invocationKindProtobufEnum
                    }
                }

                // TODO: Add else and do something like reporting issue?
            }
        }

        private fun contractExpressionProto(
            contractDescriptionElement: ContractDescriptionElement,
            contractDescription: ContractDescription
        ): ProtoBuf.Expression.Builder {
            return contractDescriptionElement.accept(object : ContractDescriptionVisitor<ProtoBuf.Expression.Builder, Unit> {
                override fun visitLogicalOr(logicalOr: LogicalOr, data: Unit): ProtoBuf.Expression.Builder {
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

                override fun visitLogicalAnd(logicalAnd: LogicalAnd, data: Unit): ProtoBuf.Expression.Builder {
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

                override fun visitLogicalNot(logicalNot: LogicalNot, data: Unit): ProtoBuf.Expression.Builder =
                    logicalNot.arg.accept(this, data).apply {
                        writeFlags(Flags.IS_NEGATED.invert(flags))
                    }

                override fun visitIsInstancePredicate(isInstancePredicate: IsInstancePredicate, data: Unit): ProtoBuf.Expression.Builder {
                    // write variable
                    val builder = visitVariableReference(isInstancePredicate.arg, data)

                    // write rhs type
                    builder.isInstanceTypeId = parentSerializer.typeId(isInstancePredicate.type)

                    // set flags
                    builder.writeFlags(Flags.getContractExpressionFlags(isInstancePredicate.isNegated, false))

                    return builder
                }

                override fun visitIsNullPredicate(isNullPredicate: IsNullPredicate, data: Unit): ProtoBuf.Expression.Builder {
                    // get builder with variable embedded into it
                    val builder = visitVariableReference(isNullPredicate.arg, data)

                    // set flags
                    builder.writeFlags(Flags.getContractExpressionFlags(isNullPredicate.isNegated, true))

                    return builder
                }

                override fun visitConstantDescriptor(constantReference: ConstantReference, data: Unit): ProtoBuf.Expression.Builder {
                    val builder = ProtoBuf.Expression.newBuilder()

                    // write constant value
                    val constantValueProtobufEnum = constantValueProtobufEnum(constantReference)
                    if (constantValueProtobufEnum != null) {
                        builder.constantValue = constantValueProtobufEnum
                    }

                    return builder
                }

                override fun visitVariableReference(variableReference: VariableReference, data: Unit): ProtoBuf.Expression.Builder {
                    val builder = ProtoBuf.Expression.newBuilder()

                    val descriptor = variableReference.descriptor
                    val indexOfParameter = when (descriptor) {
                        is ReceiverParameterDescriptor -> 0

                        is ValueParameterDescriptor ->
                            contractDescription.ownerFunction.valueParameters.indexOf(descriptor).takeIf { it != -1 }?.inc()

                        else -> null
                    }

                    builder.valueParameterReference = indexOfParameter ?: return builder

                    return builder
                }
            }, Unit)
        }

        private fun ProtoBuf.Expression.Builder.writeFlags(newFlagsValue: Int) {
            if (flags != newFlagsValue) {
                flags = newFlagsValue
            }
        }

        private fun invocationKindProtobufEnum(kind: InvocationKind): ProtoBuf.Effect.InvocationKind? = when (kind) {
            InvocationKind.AT_MOST_ONCE -> ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE
            InvocationKind.EXACTLY_ONCE -> ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE
            InvocationKind.AT_LEAST_ONCE -> ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE
            InvocationKind.UNKNOWN -> null
        }

        private fun constantValueProtobufEnum(constantReference: ConstantReference): ProtoBuf.Expression.ConstantValue? =
            when (constantReference) {
                BooleanConstantReference.TRUE -> ProtoBuf.Expression.ConstantValue.TRUE
                BooleanConstantReference.FALSE -> ProtoBuf.Expression.ConstantValue.FALSE
                ConstantReference.NULL -> ProtoBuf.Expression.ConstantValue.NULL
                ConstantReference.NOT_NULL -> throw IllegalStateException(
                    "Internal error during serialization of function contract: NOT_NULL constant isn't denotable in protobuf format. " +
                            "Its serialization should be handled at higher level"
                )
                ConstantReference.WILDCARD -> null
                else -> throw IllegalArgumentException("Unknown constant: $constantReference")
            }
    }
}