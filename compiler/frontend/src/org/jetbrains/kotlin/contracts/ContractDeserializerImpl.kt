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

package org.jetbrains.kotlin.contracts

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.contracts.description.expressions.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.isInstanceType
import org.jetbrains.kotlin.serialization.deserialization.ContractDeserializer
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.TypeDeserializer
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addIfNotNull

class ContractDeserializerImpl(private val configuration: DeserializationConfiguration) : ContractDeserializer {
    override fun deserializeContractFromFunction(
        proto: ProtoBuf.Function,
        ownerFunction: FunctionDescriptor,
        typeTable: TypeTable,
        typeDeserializer: TypeDeserializer
    ): Pair<CallableDescriptor.UserDataKey<*>, LazyContractProvider>? {
        if (!proto.hasContract()) return null

        if (!configuration.readDeserializedContracts) return null

        val worker = ContractDeserializationWorker(typeTable, typeDeserializer, ownerFunction)
        val contract = worker.deserializeContract(proto.contract)
        return ContractProviderKey to LazyContractProvider.createInitialized(contract)
    }

    private class ContractDeserializationWorker(
        private val typeTable: TypeTable,
        private val typeDeserializer: TypeDeserializer,
        private val ownerFunction: FunctionDescriptor
    ) {

        fun deserializeContract(proto: ProtoBuf.Contract): ContractDescription? {
            val effects = proto.effectList.map { deserializePossiblyConditionalEffect(it) ?: return null }
            return ContractDescription(effects, ownerFunction)
        }

        private fun deserializePossiblyConditionalEffect(proto: ProtoBuf.Effect): EffectDeclaration? {
            if (proto.hasConclusionOfConditionalEffect()) {
                // conditional effect
                val conclusion = deserializeExpression(proto.conclusionOfConditionalEffect) ?: return null
                val effect = deserializeSimpleEffect(proto) ?: return null
                return ConditionalEffectDeclaration(effect, conclusion)
            }
            return deserializeSimpleEffect(proto)
        }

        private fun deserializeSimpleEffect(proto: ProtoBuf.Effect): EffectDeclaration? {
            val type = if (proto.hasEffectType()) proto.effectType else return null
            return when (type!!) {
                ProtoBuf.Effect.EffectType.RETURNS_CONSTANT -> {
                    val argument = proto.effectConstructorArgumentList.getOrNull(0)
                    val returnValue =
                        if (argument == null) ConstantReference.WILDCARD else deserializeExpression(argument) as? ConstantReference
                                ?: return null
                    ReturnsEffectDeclaration(returnValue)
                }

                ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL -> {
                    ReturnsEffectDeclaration(ConstantReference.NOT_NULL)
                }

                ProtoBuf.Effect.EffectType.CALLS -> {
                    val argument = proto.effectConstructorArgumentList.getOrNull(0) ?: return null
                    val callable = extractVariable(argument) ?: return null
                    val invocationKind = if (proto.hasKind())
                        proto.kind.toDescriptorInvocationKind() ?: return null
                    else
                        InvocationKind.UNKNOWN
                    CallsEffectDeclaration(callable, invocationKind)
                }
            }
        }

        private fun deserializeExpression(proto: ProtoBuf.Expression): BooleanExpression? {
            val primitiveType = getPrimitiveType(proto)
            val primitiveExpression = extractPrimitiveExpression(proto, primitiveType)

            val complexType = getComplexType(proto)
            val childs: MutableList<BooleanExpression> = mutableListOf()
            childs.addIfNotNull(primitiveExpression)

            return when (complexType) {
                ComplexExpressionType.AND_SEQUENCE -> {
                    proto.andArgumentList.mapTo(childs) { deserializeExpression(it) ?: return null }
                    childs.reduce { acc, booleanExpression -> LogicalAnd(acc, booleanExpression) }
                }

                ComplexExpressionType.OR_SEQUENCE -> {
                    proto.orArgumentList.mapTo(childs) { deserializeExpression(it) ?: return null }
                    childs.reduce { acc, booleanExpression -> LogicalOr(acc, booleanExpression) }
                }

                null -> primitiveExpression
            }
        }

        private fun extractPrimitiveExpression(proto: ProtoBuf.Expression, primitiveType: PrimitiveExpressionType?): BooleanExpression? {
            val isInverted = Flags.IS_NEGATED.get(proto.flags)

            return when (primitiveType) {
                PrimitiveExpressionType.VALUE_PARAMETER_REFERENCE, PrimitiveExpressionType.RECEIVER_REFERENCE -> {
                    (extractVariable(proto) as? BooleanVariableReference?)?.invertIfNecessary(isInverted)
                }

                PrimitiveExpressionType.CONSTANT ->
                    (deserializeConstant(proto.constantValue) as? BooleanConstantReference)?.invertIfNecessary(isInverted)

                PrimitiveExpressionType.INSTANCE_CHECK -> {
                    val variable = extractVariable(proto) ?: return null
                    val type = extractType(proto) ?: return null
                    IsInstancePredicate(variable, type, isInverted)
                }

                PrimitiveExpressionType.NULLABILITY_CHECK -> {
                    val variable = extractVariable(proto) ?: return null
                    IsNullPredicate(variable, isInverted)
                }

                null -> null
            }
        }

        private fun BooleanExpression.invertIfNecessary(shouldInvert: Boolean) = if (shouldInvert) LogicalNot(this) else this

        private fun extractVariable(proto: ProtoBuf.Expression): VariableReference? {
            if (!proto.hasValueParameterReference()) return null

            val parameterDescriptor = if (proto.valueParameterReference == 0)
                ownerFunction.extensionReceiverParameter ?: return null
            else
                ownerFunction.valueParameters.getOrNull(proto.valueParameterReference - 1) ?: return null

            return if (!KotlinBuiltIns.isBoolean(parameterDescriptor.type))
                VariableReference(parameterDescriptor)
            else
                BooleanVariableReference(parameterDescriptor)
        }

        private fun ProtoBuf.Effect.InvocationKind.toDescriptorInvocationKind(): InvocationKind? = when (this) {
            ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE -> InvocationKind.AT_MOST_ONCE
            ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE -> InvocationKind.EXACTLY_ONCE
            ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE -> InvocationKind.AT_LEAST_ONCE
        }

        private fun extractType(proto: ProtoBuf.Expression): KotlinType? {
            return typeDeserializer.type(proto.isInstanceType(typeTable) ?: return null)
        }

        private fun deserializeConstant(value: ProtoBuf.Expression.ConstantValue): ConstantReference? = when (value) {
            ProtoBuf.Expression.ConstantValue.TRUE -> BooleanConstantReference.TRUE
            ProtoBuf.Expression.ConstantValue.FALSE -> BooleanConstantReference.FALSE
            ProtoBuf.Expression.ConstantValue.NULL -> ConstantReference.NULL
        }

        private fun getComplexType(proto: ProtoBuf.Expression): ComplexExpressionType? {
            val isOrSequence = proto.orArgumentCount != 0
            val isAndSequence = proto.andArgumentCount != 0
            return when {
                isOrSequence && isAndSequence -> null
                isOrSequence -> ComplexExpressionType.OR_SEQUENCE
                isAndSequence -> ComplexExpressionType.AND_SEQUENCE
                else -> null
            }
        }

        private fun getPrimitiveType(proto: ProtoBuf.Expression): PrimitiveExpressionType? {
            // Expected to be one element, but can be empty (unknown expression) or contain several elements (invalid data)
            val expressionTypes: MutableList<PrimitiveExpressionType> = mutableListOf()

            // Check for predicates
            when {
                proto.hasValueParameterReference() && proto.hasType() ->
                    expressionTypes.add(PrimitiveExpressionType.INSTANCE_CHECK)

                proto.hasValueParameterReference() && Flags.IS_NULL_CHECK_PREDICATE.get(proto.flags) ->
                    expressionTypes.add(PrimitiveExpressionType.NULLABILITY_CHECK)
            }

            // If message contains correct predicate, then predicate's type overrides type of value,
            // even is message has one
            if (expressionTypes.isNotEmpty()) {
                return expressionTypes.singleOrNull()
            }

            // Otherwise, check if it is a value
            when {
                proto.hasValueParameterReference() && proto.valueParameterReference > 0 ->
                    expressionTypes.add(PrimitiveExpressionType.VALUE_PARAMETER_REFERENCE)

                proto.hasValueParameterReference() && proto.valueParameterReference == 0 ->
                    expressionTypes.add(PrimitiveExpressionType.RECEIVER_REFERENCE)

                proto.hasConstantValue() -> expressionTypes.add(PrimitiveExpressionType.CONSTANT)
            }

            return expressionTypes.singleOrNull()
        }

        private fun ProtoBuf.Expression.hasType(): Boolean = this.hasIsInstanceType() || this.hasIsInstanceTypeId()

        // Arguments of expressions with such types are never other expressions
        private enum class PrimitiveExpressionType {
            VALUE_PARAMETER_REFERENCE,
            RECEIVER_REFERENCE,
            CONSTANT,
            INSTANCE_CHECK,
            NULLABILITY_CHECK
        }

        // Expressions with such type can take other expressions as arguments.
        // Additionally, for performance reasons, "complex expression" and "primitive expression"
        // can co-exist in the one and the same message. If "primitive expression" is present
        // in the current message, it is treated as the first argument of "complex expression".
        private enum class ComplexExpressionType {
            AND_SEQUENCE,
            OR_SEQUENCE
        }
    }
}
