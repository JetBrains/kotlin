/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtContractEffectElementType
import org.jetbrains.kotlin.psi.stubs.elements.deserializeTypeBean
import org.jetbrains.kotlin.psi.stubs.elements.serializeTypeBean

class KotlinContractEffectStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: KtContractEffectElementType
) : KotlinPlaceHolderStubImpl<KtContractEffect>(parent, elementType), KotlinContractEffectStub

enum class KotlinContractEffectType {
    CALLS {
        override fun deserialize(dataStream: StubInputStream): KtCallsEffectDeclaration<KotlinTypeBean, Nothing?> {
            val declaration = PARAMETER_REFERENCE.deserialize(dataStream)
            val range = EventOccurrencesRange.entries[dataStream.readVarInt()]
            return KtCallsEffectDeclaration(declaration as KtValueParameterReference, range)
        }
    },
    RETURNS {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            return KtReturnsEffectDeclaration(CONSTANT.deserialize(dataStream) as KtConstantReference)
        }
    },
    CONDITIONAL {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            val descriptionElement = entries[dataStream.readVarInt()].deserialize(dataStream)
            val condition = entries[dataStream.readVarInt()].deserialize(dataStream)
            return KtConditionalEffectDeclaration(
                descriptionElement as KtEffectDeclaration,
                condition as KtBooleanExpression
            )
        }
    },
    CONDITIONAL_RETURNS {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            val argumentCondition = entries[dataStream.readVarInt()].deserialize(dataStream)
            val descriptionElement = entries[dataStream.readVarInt()].deserialize(dataStream)
            return KtConditionalReturnsDeclaration(
                argumentCondition as KtBooleanExpression,
                descriptionElement as KtEffectDeclaration,
            )
        }
    },
    HOLDS_IN {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            val condition = entries[dataStream.readVarInt()].deserialize(dataStream)
            val declaration = PARAMETER_REFERENCE.deserialize(dataStream)
            return KtHoldsInEffectDeclaration(
                condition as KtBooleanExpression,
                declaration as KtValueParameterReference
            )
        }
    },
    IS_NULL {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            return KtIsNullPredicate(
                PARAMETER_REFERENCE.deserialize(dataStream) as KtValueParameterReference,
                dataStream.readBoolean()
            )
        }
    },
    IS_INSTANCE {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            return KtIsInstancePredicate(
                PARAMETER_REFERENCE.deserialize(dataStream) as KtValueParameterReference,
                deserializeTypeBean(dataStream)!!,
                dataStream.readBoolean()
            )
        }
    },
    NOT {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            return KtLogicalNot(entries[dataStream.readVarInt()].deserialize(dataStream) as KtBooleanExpression)
        }
    },
    BOOLEAN_LOGIC {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            val kind = if (dataStream.readBoolean()) LogicOperationKind.AND else LogicOperationKind.OR
            val left = entries[dataStream.readVarInt()].deserialize(dataStream) as KtBooleanExpression
            val right = entries[dataStream.readVarInt()].deserialize(dataStream) as KtBooleanExpression
            return KtBinaryLogicExpression(left, right, kind)
        }
    },
    PARAMETER_REFERENCE {
        override fun deserialize(dataStream: StubInputStream): KtValueParameterReference<KotlinTypeBean, Nothing?> {
            return KtValueParameterReference(dataStream.readVarInt(), IGNORE_REFERENCE_PARAMETER_NAME)
        }
    },
    BOOLEAN_PARAMETER_REFERENCE {
        override fun deserialize(dataStream: StubInputStream): KtValueParameterReference<KotlinTypeBean, Nothing?> {
            return KtBooleanValueParameterReference(dataStream.readVarInt(), IGNORE_REFERENCE_PARAMETER_NAME)
        }
    },
    CONSTANT {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            return when (val str = dataStream.readNameString()!!) {
                "TRUE" -> KotlinContractConstantValues.TRUE
                "FALSE" -> KotlinContractConstantValues.FALSE
                "NULL" -> KotlinContractConstantValues.NULL
                "NOT_NULL" -> KotlinContractConstantValues.NOT_NULL
                "WILDCARD" -> KotlinContractConstantValues.WILDCARD
                else -> error("Unexpected $str")
            }
        }
    };

    abstract fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?>

    companion object {
        const val IGNORE_REFERENCE_PARAMETER_NAME = "<ignore>"
    }
}

class KotlinContractSerializationVisitor(val dataStream: StubOutputStream) :
    KtContractDescriptionVisitor<Unit, Nothing?, KotlinTypeBean, Nothing?>() {
    override fun visitConditionalEffectDeclaration(
        conditionalEffect: KtConditionalEffectDeclaration<KotlinTypeBean, Nothing?>,
        data: Nothing?
    ) {
        dataStream.writeVarInt(KotlinContractEffectType.CONDITIONAL.ordinal)
        conditionalEffect.effect.accept(this, data)
        conditionalEffect.condition.accept(this, data)
    }

    override fun visitConditionalReturnsDeclaration(
        conditionalEffect: KtConditionalReturnsDeclaration<KotlinTypeBean, Nothing?>,
        data: Nothing?,
    ) {
        dataStream.writeVarInt(KotlinContractEffectType.CONDITIONAL_RETURNS.ordinal)
        conditionalEffect.argumentsCondition.accept(this, data)
        conditionalEffect.returnsEffect.accept(this, data)
    }

    override fun visitHoldsInEffectDeclaration(
        holdsInEffect: KtHoldsInEffectDeclaration<KotlinTypeBean, Nothing?>,
        data: Nothing?,
    ) {
        dataStream.writeVarInt(KotlinContractEffectType.HOLDS_IN.ordinal)
        holdsInEffect.argumentsCondition.accept(this, data)
        dataStream.writeVarInt(holdsInEffect.valueParameterReference.parameterIndex)
    }

    override fun visitReturnsEffectDeclaration(returnsEffect: KtReturnsEffectDeclaration<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeVarInt(KotlinContractEffectType.RETURNS.ordinal)
        dataStream.writeName(returnsEffect.value.name)
    }

    override fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeVarInt(KotlinContractEffectType.CALLS.ordinal)
        dataStream.writeVarInt(callsEffect.valueParameterReference.parameterIndex)
        dataStream.writeVarInt(callsEffect.kind.ordinal)
    }

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: KtBinaryLogicExpression<KotlinTypeBean, Nothing?>,
        data: Nothing?
    ) {
        dataStream.writeVarInt(KotlinContractEffectType.BOOLEAN_LOGIC.ordinal)
        dataStream.writeBoolean(binaryLogicExpression.kind == LogicOperationKind.AND)
        binaryLogicExpression.left.accept(this, data)
        binaryLogicExpression.right.accept(this, data)
    }

    override fun visitLogicalNot(logicalNot: KtLogicalNot<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeVarInt(KotlinContractEffectType.NOT.ordinal)
        logicalNot.arg.accept(this, data)
    }

    override fun visitIsInstancePredicate(isInstancePredicate: KtIsInstancePredicate<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeVarInt(KotlinContractEffectType.IS_INSTANCE.ordinal)
        dataStream.writeVarInt(isInstancePredicate.arg.parameterIndex)
        serializeTypeBean(dataStream, isInstancePredicate.type)
        dataStream.writeBoolean(isInstancePredicate.isNegated)
    }

    override fun visitIsNullPredicate(isNullPredicate: KtIsNullPredicate<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeVarInt(KotlinContractEffectType.IS_NULL.ordinal)
        dataStream.writeVarInt(isNullPredicate.arg.parameterIndex)
        dataStream.writeBoolean(isNullPredicate.isNegated)
    }


    override fun visitConstantDescriptor(constantReference: KtConstantReference<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeVarInt(KotlinContractEffectType.CONSTANT.ordinal)
        dataStream.writeName(constantReference.name)
    }

    override fun visitValueParameterReference(valueParameterReference: KtValueParameterReference<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeVarInt(KotlinContractEffectType.PARAMETER_REFERENCE.ordinal)
        dataStream.writeVarInt(valueParameterReference.parameterIndex)
    }

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: KtBooleanValueParameterReference<KotlinTypeBean, Nothing?>,
        data: Nothing?
    ) {
        dataStream.writeVarInt(KotlinContractEffectType.BOOLEAN_PARAMETER_REFERENCE.ordinal)
        dataStream.writeVarInt(booleanValueParameterReference.parameterIndex)
    }
}

object KotlinContractConstantValues {
    val NULL = KtConstantReference<KotlinTypeBean, Nothing?>("NULL")
    val WILDCARD = KtConstantReference<KotlinTypeBean, Nothing?>("WILDCARD")
    val NOT_NULL = KtConstantReference<KotlinTypeBean, Nothing?>("NOT_NULL")

    val TRUE = KtBooleanConstantReference<KotlinTypeBean, Nothing?>("TRUE")
    val FALSE = KtBooleanConstantReference<KotlinTypeBean, Nothing?>("FALSE")
}