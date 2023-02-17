/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.contracts.description.EffectType
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.ExpressionType
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.isInstanceType
import org.jetbrains.kotlin.psi.KtContractEffectList
import org.jetbrains.kotlin.psi.stubs.KotlinContractExpressionStub
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContractExpressionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContractEffectStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl

class ClsContractBuilder(private val typeStubBuilder: TypeClsStubBuilder, private val typeTable: TypeTable) {
    fun loadContract(proto: ProtoBuf.Contract, functionStub: KotlinFunctionStub) {
        val effectsList =
            KotlinPlaceHolderStubImpl<KtContractEffectList>(functionStub, KtStubElementTypes.CONTRACT_EFFECT_LIST)
        proto.effectList.forEach { loadPossiblyConditionalEffect(it, effectsList) }
    }

    private fun loadPossiblyConditionalEffect(
        proto: ProtoBuf.Effect,
        parentStub: StubElement<*>
    ) {
        if (proto.hasConclusionOfConditionalEffect()) {
            val effectStub = KotlinContractEffectStubImpl(parentStub, KtStubElementTypes.CONTRACT_EFFECT, EffectType.CONDITIONAL)
            val conclusionStub = KotlinContractExpressionStubImpl(effectStub, ExpressionType.CONCLUSION, "")
            loadExpression(proto.conclusionOfConditionalEffect, conclusionStub)
            loadSimpleEffect(proto, effectStub)
        } else {
            loadSimpleEffect(proto, parentStub)
        }
    }

    private fun loadSimpleEffect(
        proto: ProtoBuf.Effect,
        parentStub: StubElement<*>
    ) {
        val type: ProtoBuf.Effect.EffectType = if (proto.hasEffectType()) proto.effectType else return
        when (type) {
            ProtoBuf.Effect.EffectType.RETURNS_CONSTANT -> {
                val effectStub = KotlinContractEffectStubImpl(parentStub, KtStubElementTypes.CONTRACT_EFFECT, EffectType.RETURNS_CONSTANT)
                val argument = proto.effectConstructorArgumentList.firstOrNull()
                if (argument == null) {
                    KotlinContractExpressionStubImpl(effectStub, ExpressionType.CONST, "WILDCARD")
                } else {
                    loadExpression(argument, effectStub)
                }
            }
            ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL -> {
                KotlinContractEffectStubImpl(parentStub, KtStubElementTypes.CONTRACT_EFFECT, EffectType.RETURNS_NOT_NULL)
            }
            ProtoBuf.Effect.EffectType.CALLS -> {
                val argument = proto.effectConstructorArgumentList.firstOrNull() ?: return
                val effectStub = KotlinContractEffectStubImpl(parentStub, KtStubElementTypes.CONTRACT_EFFECT, EffectType.CALLS)
                extractVariable(argument, effectStub) ?: return
                val invocationKind = if (proto.hasKind())
                    proto.kind.toDescriptorInvocationKind()
                else
                    EventOccurrencesRange.UNKNOWN
                KotlinContractExpressionStubImpl(effectStub, ExpressionType.CONST, invocationKind.name)
            }
        }
    }

    private fun loadExpression(proto: ProtoBuf.Expression, parentStub: StubElement<*>) {
        val primitiveType = getPrimitiveType(proto)

        when (getComplexType(proto)) {
            ComplexExpressionType.AND_SEQUENCE -> {
                val andExpr = KotlinContractExpressionStubImpl(parentStub, ExpressionType.AND, "")
                extractPrimitiveExpression(proto, primitiveType, andExpr)
                proto.andArgumentList.forEach { loadExpression(it, andExpr) }
            }

            ComplexExpressionType.OR_SEQUENCE -> {
                val orExpr = KotlinContractExpressionStubImpl(parentStub, ExpressionType.OR, "")
                extractPrimitiveExpression(proto, primitiveType, orExpr)
                proto.andArgumentList.forEach { loadExpression(it, orExpr) }
            }

            null -> {
                extractPrimitiveExpression(proto, primitiveType, parentStub)
            }
        }
    }

    private fun extractPrimitiveExpression(
        proto: ProtoBuf.Expression,
        primitiveType: PrimitiveExpressionType?,
        parentStub: StubElement<*>
    ) {
        val isInverted = Flags.IS_NEGATED[proto.flags]

        when (primitiveType) {
            PrimitiveExpressionType.VALUE_PARAMETER_REFERENCE, PrimitiveExpressionType.RECEIVER_REFERENCE -> {
                extractVariable(proto, parentStub.invertIfNecessary(isInverted))
            }

            PrimitiveExpressionType.CONSTANT ->
                loadConstant(proto.constantValue, parentStub.invertIfNecessary(isInverted))

            PrimitiveExpressionType.INSTANCE_CHECK -> {
                val instanceStub =
                    KotlinContractExpressionStubImpl(parentStub.invertIfNecessary(isInverted), ExpressionType.IS_INSTANCE, isInverted.toString())
                extractVariable(proto, instanceStub) ?: return
                typeStubBuilder.createTypeReferenceStub(instanceStub, proto.isInstanceType(typeTable) ?: return)
            }

            PrimitiveExpressionType.NULLABILITY_CHECK -> {
                val nullabilityStub = KotlinContractExpressionStubImpl(parentStub, ExpressionType.NULLABILITY, isInverted.toString())
                extractVariable(proto, nullabilityStub) ?: return
            }
            null -> {}
        }
    }

    private fun StubElement<*>.invertIfNecessary(shouldInvert: Boolean): StubElement<*> {
        return if (shouldInvert) {
            KotlinContractExpressionStubImpl(this, ExpressionType.NOT, "")
        } else this
    }


    private fun extractVariable(
        proto: ProtoBuf.Expression,
        parent: StubElement<*>
    ): KotlinContractExpressionStub? {
        if (!proto.hasValueParameterReference()) return null

        val valueParameterIndex = proto.valueParameterReference - 1

        return KotlinContractExpressionStubImpl(parent, ExpressionType.PARAM, valueParameterIndex.toString())
    }

    private fun ProtoBuf.Effect.InvocationKind.toDescriptorInvocationKind(): EventOccurrencesRange = when (this) {
        ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE -> EventOccurrencesRange.AT_MOST_ONCE
        ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE -> EventOccurrencesRange.EXACTLY_ONCE
        ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE -> EventOccurrencesRange.AT_LEAST_ONCE
    }

    private fun loadConstant(value: ProtoBuf.Expression.ConstantValue, parent: StubElement<*>): KotlinContractExpressionStub =
        when (value) {
            ProtoBuf.Expression.ConstantValue.TRUE -> KotlinContractExpressionStubImpl(parent, ExpressionType.CONST, "TRUE")
            ProtoBuf.Expression.ConstantValue.FALSE -> KotlinContractExpressionStubImpl(parent, ExpressionType.CONST, "FALSE")
            ProtoBuf.Expression.ConstantValue.NULL -> KotlinContractExpressionStubImpl(parent, ExpressionType.CONST, "NULL")
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

            proto.hasValueParameterReference() && Flags.IS_NULL_CHECK_PREDICATE[proto.flags] ->
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