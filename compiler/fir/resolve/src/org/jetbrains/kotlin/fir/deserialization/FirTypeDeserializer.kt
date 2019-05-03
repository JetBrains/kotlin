/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.resolve.toTypeProjection
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.Variance
import java.util.*

class FirTypeDeserializer(
    val session: FirSession,
    val nameResolver: NameResolver,
    val typeTable: TypeTable,
    typeParameterProtos: List<ProtoBuf.TypeParameter>,
    val parent: FirTypeDeserializer?
) {

    private fun computeClassifier(fqNameIndex: Int): ConeClassLikeLookupTag? {
        try {
            val id = nameResolver.getClassId(fqNameIndex)
            return ConeClassLikeLookupTagImpl(id)
        } catch (e: Throwable) {
            throw RuntimeException("Looking up for ${nameResolver.getClassId(fqNameIndex)}", e)
        }
    }

    fun type(proto: ProtoBuf.Type): ConeKotlinType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val id = nameResolver.getString(proto.flexibleTypeCapabilitiesId)
            val lowerBound = simpleType(proto)
            val upperBound = simpleType(proto.flexibleUpperBound(typeTable)!!)
            return ConeFlexibleType(lowerBound!!, upperBound!!)
            //c.components.flexibleTypeDeserializer.create(proto, id, lowerBound, upperBound)
        }

        return simpleType(proto) ?: ConeKotlinErrorType("?!id:0")
    }


    private fun typeParameterSymbol(typeParameterId: Int): ConeTypeParameterLookupTag? =
        typeParameterDescriptors[typeParameterId] ?: parent?.typeParameterSymbol(typeParameterId)


    private fun ProtoBuf.TypeParameter.Variance.convertVariance(): Variance {
        return when (this) {
            ProtoBuf.TypeParameter.Variance.IN -> Variance.IN_VARIANCE
            ProtoBuf.TypeParameter.Variance.OUT -> Variance.OUT_VARIANCE
            ProtoBuf.TypeParameter.Variance.INV -> Variance.INVARIANT
        }
    }

    private val typeParameterDescriptors =
        if (typeParameterProtos.isEmpty()) {
            mapOf<Int, ConeTypeParameterSymbol>()
        } else {
            val result = LinkedHashMap<Int, ConeTypeParameterSymbol>()
            for ((index, proto) in typeParameterProtos.withIndex()) {
                if (!proto.hasId()) continue
                val name = nameResolver.getName(proto.name)
                val symbol = FirTypeParameterSymbol()
                FirTypeParameterImpl(
                    session,
                    null,
                    symbol,
                    name,
                    proto.variance.convertVariance(),
                    proto.reified
                )
                result[proto.id] = symbol
            }
            result
        }


    init {
        for ((index, proto) in typeParameterProtos.withIndex()) {
            if (!proto.hasId()) continue
            val symbol = typeParameterDescriptors[proto.id] as FirTypeParameterSymbol
            val declaration = symbol.firUnsafe<FirTypeParameterImpl>()
            declaration.apply {
                proto.upperBoundList.mapTo(bounds) {
                    FirResolvedTypeRefImpl(session, null, type(it), emptyList())
                }
            }
        }
    }

    val ownTypeParameters: List<ConeTypeParameterSymbol>
        get() = typeParameterDescriptors.values.toList()


    fun ConeClassLikeSymbol.typeParameters(): List<ConeTypeParameterSymbol> = when (this) {
        is FirTypeAliasSymbol -> fir.typeParameters
        is FirClassSymbol -> fir.typeParameters
        else -> error("?!id:2")
    }.map {
        it.symbol
    }

    fun simpleType(proto: ProtoBuf.Type): ConeLookupTagBasedType? {

        val constructor = typeSymbol(proto) ?: return null
        if (constructor is ConeTypeParameterLookupTag) return ConeTypeParameterTypeImpl(constructor, isNullable = false)
        if (constructor !is ConeClassLikeLookupTag) return null

        fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
            argumentList + outerType(typeTable)?.collectAllArguments().orEmpty()

        val arguments = proto.collectAllArguments().map(this::typeArgument).toTypedArray()

        val simpleType = if (Flags.SUSPEND_TYPE.get(proto.flags)) {
            //createSuspendFunctionType(annotations, constructor, arguments, proto.nullable)
            ConeClassErrorType("createSuspendFunctionType not supported")
        } else {
            ConeClassTypeImpl(constructor, arguments, isNullable = proto.nullable)
        }

        val abbreviatedTypeProto = proto.abbreviatedType(typeTable) ?: return simpleType

        return ConeAbbreviatedTypeImpl(typeSymbol(abbreviatedTypeProto) as ConeClassLikeLookupTag, arguments, isNullable = false)

    }

    private fun typeSymbol(proto: ProtoBuf.Type): ConeClassifierLookupTag? {

        return when {
            proto.hasClassName() -> computeClassifier(proto.className)
            proto.hasTypeAliasName() -> computeClassifier(proto.typeAliasName)
            proto.hasTypeParameter() -> typeParameterSymbol(proto.typeParameter)
            proto.hasTypeParameterName() -> {
                val name = nameResolver.getString(proto.typeParameterName)

                // TODO: Optimize
                ownTypeParameters.find { it.name.asString() == name }
            }
            else -> null
        }
    }


    private fun typeArgument(typeArgumentProto: ProtoBuf.Type.Argument): ConeKotlinTypeProjection {
        if (typeArgumentProto.projection == ProtoBuf.Type.Argument.Projection.STAR) {
            return ConeStarProjection
        }

        val variance = ProtoEnumFlags.variance(typeArgumentProto.projection)
        val type = typeArgumentProto.type(typeTable) ?: return ConeKotlinErrorType("No type recorded")

        val coneType = type(type)
        return coneType.toTypeProjection(variance)
    }

}
