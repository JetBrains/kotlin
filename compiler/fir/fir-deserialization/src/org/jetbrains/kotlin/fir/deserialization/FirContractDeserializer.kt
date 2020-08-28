/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.builder.buildEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.builder.buildResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.toFirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.LogicOperationKind
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.isInstanceType
import org.jetbrains.kotlin.utils.addIfNotNull

class FirContractDeserializer(private val c: FirDeserializationContext) {
    fun loadContract(proto: ProtoBuf.Contract, owner: FirContractDescriptionOwner): FirContractDescription? {
        val effects = proto.effectList.map { loadPossiblyConditionalEffect(it, owner) ?: return null }
        return buildResolvedContractDescription {
            this.effects += effects.map { it.toFirEffectDeclaration() }
        }
    }

    private fun loadPossiblyConditionalEffect(
        proto: ProtoBuf.Effect,
        owner: FirContractDescriptionOwner
    ): ConeEffectDeclaration? {
        if (proto.hasConclusionOfConditionalEffect()) {
            val conclusion = loadExpression(proto.conclusionOfConditionalEffect, owner) ?: return null
            val effect = loadSimpleEffect(proto, owner) ?: return null
            return ConeConditionalEffectDeclaration(effect, conclusion)
        }
        return loadSimpleEffect(proto, owner)
    }

    private fun loadSimpleEffect(proto: ProtoBuf.Effect, owner: FirContractDescriptionOwner): ConeEffectDeclaration? {
        val type: ProtoBuf.Effect.EffectType = if (proto.hasEffectType()) proto.effectType else return null
        return when(type) {
            ProtoBuf.Effect.EffectType.RETURNS_CONSTANT -> {
                val argument = proto.effectConstructorArgumentList.firstOrNull()
                val returnValue = if (argument == null) {
                    ConeConstantReference.WILDCARD
                } else {
                    loadExpression(argument, owner) as? ConeConstantReference ?: return null
                }
                ConeReturnsEffectDeclaration(returnValue)
            }
            ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL -> {
                ConeReturnsEffectDeclaration(ConeConstantReference.NOT_NULL)
            }
            ProtoBuf.Effect.EffectType.CALLS -> {
                val argument = proto.effectConstructorArgumentList.firstOrNull() ?: return null
                val callable = extractVariable(argument, owner) ?: return null
                val invocationKind = if (proto.hasKind())
                    proto.kind.toDescriptorInvocationKind() ?: return null
                else
                    EventOccurrencesRange.UNKNOWN
                ConeCallsEffectDeclaration(callable, invocationKind)
            }
        }
    }

    private fun loadExpression(proto: ProtoBuf.Expression, owner: FirContractDescriptionOwner): ConeBooleanExpression? {
        val primitiveType = getPrimitiveType(proto)
        val primitiveExpression = extractPrimitiveExpression(proto, primitiveType, owner)

        val complexType = getComplexType(proto)
        val childs: MutableList<ConeBooleanExpression> = mutableListOf()
        childs.addIfNotNull(primitiveExpression)

        return when (complexType) {
            ComplexExpressionType.AND_SEQUENCE -> {
                proto.andArgumentList.mapTo(childs) { loadExpression(it, owner) ?: return null }
                childs.reduce { acc, booleanExpression -> ConeBinaryLogicExpression(acc, booleanExpression, LogicOperationKind.AND) }
            }

            ComplexExpressionType.OR_SEQUENCE -> {
                proto.orArgumentList.mapTo(childs) { loadExpression(it, owner) ?: return null }
                childs.reduce { acc, booleanExpression -> ConeBinaryLogicExpression(acc, booleanExpression, LogicOperationKind.OR) }
            }

            null -> primitiveExpression
        }
    }

    private fun extractPrimitiveExpression(proto: ProtoBuf.Expression, primitiveType: PrimitiveExpressionType?, owner: FirContractDescriptionOwner): ConeBooleanExpression? {
        val isInverted = Flags.IS_NEGATED.get(proto.flags)

        return when (primitiveType) {
            PrimitiveExpressionType.VALUE_PARAMETER_REFERENCE, PrimitiveExpressionType.RECEIVER_REFERENCE -> {
                (extractVariable(proto, owner) as? ConeBooleanValueParameterReference?)?.invertIfNecessary(isInverted)
            }

            PrimitiveExpressionType.CONSTANT ->
                (loadConstant(proto.constantValue) as? ConeBooleanConstantReference)?.invertIfNecessary(isInverted)

            PrimitiveExpressionType.INSTANCE_CHECK -> {
                val variable = extractVariable(proto, owner) ?: return null
                val type = extractType(proto) ?: return null
                ConeIsInstancePredicate(variable, type, isInverted)
            }

            PrimitiveExpressionType.NULLABILITY_CHECK -> {
                val variable = extractVariable(proto, owner) ?: return null
                ConeIsNullPredicate(variable, isInverted)
            }

            null -> null
        }
    }

    private fun ConeBooleanExpression.invertIfNecessary(shouldInvert: Boolean): ConeBooleanExpression =
        if (shouldInvert) ConeLogicalNot(this) else this

    private fun extractVariable(proto: ProtoBuf.Expression, owner: FirContractDescriptionOwner): ConeValueParameterReference? {
        if (!proto.hasValueParameterReference()) return null

        val ownerFunction = owner as FirSimpleFunction

        val valueParameterIndex = proto.valueParameterReference - 1

        val name: String
        val typeRef = if (valueParameterIndex < 0) {
            name = "this"
            ownerFunction.receiverTypeRef
        } else {
            val parameter = ownerFunction.valueParameters.getOrNull(valueParameterIndex) ?: return null
            name = parameter.name.asString()
            parameter.returnTypeRef
        } ?: return null

        return if (!typeRef.isBoolean)
            ConeValueParameterReference(valueParameterIndex, name)
        else
            ConeBooleanValueParameterReference(valueParameterIndex, name)
    }

    private fun ProtoBuf.Effect.InvocationKind.toDescriptorInvocationKind(): EventOccurrencesRange? = when (this) {
        ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE -> EventOccurrencesRange.AT_MOST_ONCE
        ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE -> EventOccurrencesRange.EXACTLY_ONCE
        ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE -> EventOccurrencesRange.AT_LEAST_ONCE
    }

    private fun extractType(proto: ProtoBuf.Expression): ConeKotlinType? {
        return c.typeDeserializer.type(proto.isInstanceType(c.typeTable) ?: return null, ConeAttributes.Empty)
    }

    private fun loadConstant(value: ProtoBuf.Expression.ConstantValue): ConeConstantReference? = when (value) {
        ProtoBuf.Expression.ConstantValue.TRUE -> ConeBooleanConstantReference.TRUE
        ProtoBuf.Expression.ConstantValue.FALSE -> ConeBooleanConstantReference.FALSE
        ProtoBuf.Expression.ConstantValue.NULL -> ConeConstantReference.NULL
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
