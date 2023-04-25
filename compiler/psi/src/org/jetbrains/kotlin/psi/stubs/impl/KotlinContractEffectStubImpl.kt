/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.psi.stubs.elements.KtUserTypeElementType.deserializeType
import org.jetbrains.kotlin.psi.stubs.elements.KtUserTypeElementType.serializeType

class KotlinContractEffectStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: KtContractEffectElementType
) : KotlinPlaceHolderStubImpl<KtContractEffect>(parent, elementType), KotlinContractEffectStub

enum class KotlinContractEffectType {
    CALLS {
        override fun deserialize(dataStream: StubInputStream): KtCallsEffectDeclaration<KotlinTypeBean, Nothing?> {
            val declaration = PARAMETER_REFERENCE.deserialize(dataStream)
            val range = EventOccurrencesRange.values()[dataStream.readInt()]
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
            val descriptionElement = values()[dataStream.readInt()].deserialize(dataStream)
            val condition = values()[dataStream.readInt()].deserialize(dataStream)
            return KtConditionalEffectDeclaration(
                descriptionElement as KtEffectDeclaration,
                condition as KtBooleanExpression
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
                deserializeType(dataStream)!!,
                dataStream.readBoolean()
            )
        }
    },
    NOT {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            return KtLogicalNot(values()[dataStream.readInt()].deserialize(dataStream) as KtBooleanExpression)
        }
    },
    BOOLEAN_LOGIC {
        override fun deserialize(dataStream: StubInputStream): KtContractDescriptionElement<KotlinTypeBean, Nothing?> {
            val kind = if (dataStream.readBoolean()) LogicOperationKind.AND else LogicOperationKind.OR
            val left = values()[dataStream.readInt()].deserialize(dataStream) as KtBooleanExpression
            val right = values()[dataStream.readInt()].deserialize(dataStream) as KtBooleanExpression
            return KtBinaryLogicExpression(left, right, kind)
        }
    },
    PARAMETER_REFERENCE {
        override fun deserialize(dataStream: StubInputStream): KtValueParameterReference<KotlinTypeBean, Nothing?> {
            return KtValueParameterReference(dataStream.readInt(), IGNORE_REFERENCE_PARAMETER_NAME)
        }
    },
    BOOLEAN_PARAMETER_REFERENCE {
        override fun deserialize(dataStream: StubInputStream): KtValueParameterReference<KotlinTypeBean, Nothing?> {
            return KtBooleanValueParameterReference(dataStream.readInt(), IGNORE_REFERENCE_PARAMETER_NAME)
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
        dataStream.writeInt(KotlinContractEffectType.CONDITIONAL.ordinal)
        conditionalEffect.effect.accept(this, data)
        conditionalEffect.condition.accept(this, data)
    }

    override fun visitReturnsEffectDeclaration(returnsEffect: KtReturnsEffectDeclaration<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeInt(KotlinContractEffectType.RETURNS.ordinal)
        dataStream.writeName(returnsEffect.value.name)
    }

    override fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeInt(KotlinContractEffectType.CALLS.ordinal)
        dataStream.writeInt(callsEffect.valueParameterReference.parameterIndex)
        dataStream.writeInt(callsEffect.kind.ordinal)
    }

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: KtBinaryLogicExpression<KotlinTypeBean, Nothing?>,
        data: Nothing?
    ) {
        dataStream.writeInt(KotlinContractEffectType.BOOLEAN_LOGIC.ordinal)
        dataStream.writeBoolean(binaryLogicExpression.kind == LogicOperationKind.AND)
        binaryLogicExpression.left.accept(this, data)
        binaryLogicExpression.right.accept(this, data)
    }

    override fun visitLogicalNot(logicalNot: KtLogicalNot<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeInt(KotlinContractEffectType.NOT.ordinal)
        logicalNot.arg.accept(this, data)
    }

    override fun visitIsInstancePredicate(isInstancePredicate: KtIsInstancePredicate<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeInt(KotlinContractEffectType.IS_INSTANCE.ordinal)
        dataStream.writeInt(isInstancePredicate.arg.parameterIndex)
        serializeType(dataStream, isInstancePredicate.type)
        dataStream.writeBoolean(isInstancePredicate.isNegated)
    }

    override fun visitIsNullPredicate(isNullPredicate: KtIsNullPredicate<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeInt(KotlinContractEffectType.IS_NULL.ordinal)
        dataStream.writeInt(isNullPredicate.arg.parameterIndex)
        dataStream.writeBoolean(isNullPredicate.isNegated)
    }


    override fun visitConstantDescriptor(constantReference: KtConstantReference<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeInt(KotlinContractEffectType.CONSTANT.ordinal)
        dataStream.writeName(constantReference.name)
    }

    override fun visitValueParameterReference(valueParameterReference: KtValueParameterReference<KotlinTypeBean, Nothing?>, data: Nothing?) {
        dataStream.writeInt(KotlinContractEffectType.PARAMETER_REFERENCE.ordinal)
        dataStream.writeInt(valueParameterReference.parameterIndex)
    }

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: KtBooleanValueParameterReference<KotlinTypeBean, Nothing?>,
        data: Nothing?
    ) {
        dataStream.writeInt(KotlinContractEffectType.BOOLEAN_PARAMETER_REFERENCE.ordinal)
        dataStream.writeInt(booleanValueParameterReference.parameterIndex)
    }
}

object KotlinContractConstantValues {
    val NULL = KtConstantReference<KotlinTypeBean, Nothing?>("NULL")
    val WILDCARD = KtConstantReference<KotlinTypeBean, Nothing?>("WILDCARD")
    val NOT_NULL = KtConstantReference<KotlinTypeBean, Nothing?>("NOT_NULL")

    val TRUE = KtBooleanConstantReference<KotlinTypeBean, Nothing?>("TRUE")
    val FALSE = KtBooleanConstantReference<KotlinTypeBean, Nothing?>("FALSE")
}