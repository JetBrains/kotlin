/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.contracts.description.KtBooleanValueParameterReference
import org.jetbrains.kotlin.contracts.description.KtConstantReference
import org.jetbrains.kotlin.contracts.description.KtValueParameterReference
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.toFirElement
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.isInstanceType
import org.jetbrains.kotlin.serialization.deserialization.ProtoBufContractDeserializer

class FirContractDeserializer(private val c: FirDeserializationContext) :
    ProtoBufContractDeserializer<ConeKotlinType, ConeDiagnostic, FirContractDescriptionOwner>() {
    fun loadContract(proto: ProtoBuf.Contract, owner: FirContractDescriptionOwner): FirContractDescription? {
        val effects = proto.effectList.map { loadPossiblyConditionalEffect(it, owner) ?: return null }
        return buildResolvedContractDescription {
            this.effects += effects.map { it.toFirElement() }
        }
    }

    override fun extractType(proto: ProtoBuf.Expression): ConeKotlinType? {
        return c.typeDeserializer.type(proto.isInstanceType(c.typeTable) ?: return null)
    }

    override fun extractVariable(
        valueParameterIndex: Int,
        owner: FirContractDescriptionOwner
    ): KtValueParameterReference<ConeKotlinType, ConeDiagnostic>?  {
        val name: String
        val ownerFunction = owner as FirSimpleFunction
        val typeRef = if (valueParameterIndex < 0) {
            name = "this"
            ownerFunction.receiverParameter?.typeRef
        } else {
            val parameter = ownerFunction.valueParameters.getOrNull(valueParameterIndex) ?: return null
            name = parameter.name.asString()
            parameter.returnTypeRef
        } ?: return null

        return if (!typeRef.isBoolean)
            KtValueParameterReference(valueParameterIndex, name)
        else
            KtBooleanValueParameterReference(valueParameterIndex, name)
    }

    override fun loadConstant(value: ProtoBuf.Expression.ConstantValue): KtConstantReference<ConeKotlinType, ConeDiagnostic> {
        return when (value) {
            ProtoBuf.Expression.ConstantValue.TRUE -> ConeContractConstantValues.TRUE
            ProtoBuf.Expression.ConstantValue.FALSE -> ConeContractConstantValues.FALSE
            ProtoBuf.Expression.ConstantValue.NULL -> ConeContractConstantValues.NULL
        }
    }

    override fun getNotNull(): KtConstantReference<ConeKotlinType, ConeDiagnostic> {
        return ConeContractConstantValues.NOT_NULL
    }

    override fun getWildcard(): KtConstantReference<ConeKotlinType, ConeDiagnostic> {
        return ConeContractConstantValues.WILDCARD
    }
}
