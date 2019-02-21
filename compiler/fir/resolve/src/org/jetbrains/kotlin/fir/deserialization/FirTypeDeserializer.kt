/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toTypeProjection
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.LibraryTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import java.lang.RuntimeException
import java.util.*

class FirTypeDeserializer(
    val nameResolver: NameResolver,
    val typeTable: TypeTable,
    val symbolProvider: FirSymbolProvider,
    typeParameterProtos: List<ProtoBuf.TypeParameter>,
    val parent: FirTypeDeserializer?
) {

    private fun computeClassifier(fqNameIndex: Int): ConeSymbol? {
        try {
            val id = nameResolver.getClassId(fqNameIndex)
            return symbolProvider.getClassLikeSymbolByFqName(id)
        } catch (e: Throwable) {
            throw RuntimeException("Looking up for ${nameResolver.getClassId(fqNameIndex)}", e)
        }
    }

    fun type(proto: ProtoBuf.Type): ConeKotlinType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val id = nameResolver.getString(proto.flexibleTypeCapabilitiesId)
            val lowerBound = classLikeType(proto)
            val upperBound = classLikeType(proto.flexibleUpperBound(typeTable)!!)
            return ConeFlexibleType(lowerBound!!, upperBound!!)
            //c.components.flexibleTypeDeserializer.create(proto, id, lowerBound, upperBound)
        }

        return classLikeType(proto) ?: ConeKotlinErrorType("?!id:0")
    }


    private fun typeParameterSymbol(typeParameterId: Int): ConeTypeParameterSymbol? =
        typeParameterDescriptors[typeParameterId] ?: parent?.typeParameterSymbol(typeParameterId)

    private val typeParameterDescriptors =
        if (typeParameterProtos.isEmpty()) {
            mapOf<Int, ConeTypeParameterSymbol>()
        } else {
            val result = LinkedHashMap<Int, ConeTypeParameterSymbol>()
            for ((index, proto) in typeParameterProtos.withIndex()) {
                result[proto.id] = LibraryTypeParameterSymbol(nameResolver.getName(proto.name))
            }
            result
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

    fun classLikeType(proto: ProtoBuf.Type): ConeClassLikeType? {

        val constructor = typeSymbol(proto) as? ConeClassLikeSymbol ?: return null
//        if (ErrorUtils.isError(constructor.declarationDescriptor)) {
//            return ErrorUtils.createErrorTypeWithCustomConstructor(constructor.toString(), constructor)
//        }

        fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
            argumentList + outerType(typeTable)?.collectAllArguments().orEmpty()

        val arguments = proto.collectAllArguments().mapIndexed { index, proto ->
            typeArgument(constructor.typeParameters().getOrNull(index), proto)
        }.toTypedArray()

        val simpleType = if (Flags.SUSPEND_TYPE.get(proto.flags)) {
            //createSuspendFunctionType(annotations, constructor, arguments, proto.nullable)
            ConeClassErrorType("createSuspendFunctionType not supported")
        } else {
            ConeClassTypeImpl(constructor, arguments, isNullable = false)
        }

        val abbreviatedTypeProto = proto.abbreviatedType(typeTable) ?: return simpleType

        return ConeAbbreviatedTypeImpl(typeSymbol(abbreviatedTypeProto) as ConeClassLikeSymbol, arguments, simpleType, isNullable = false)

    }

    private fun typeSymbol(proto: ProtoBuf.Type): ConeSymbol? {

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


    private fun typeArgument(parameter: ConeTypeParameterSymbol?, typeArgumentProto: ProtoBuf.Type.Argument): ConeKotlinTypeProjection {
        if (typeArgumentProto.projection == ProtoBuf.Type.Argument.Projection.STAR) {
            return StarProjection
        }

        val variance = ProtoEnumFlags.variance(typeArgumentProto.projection)
        val type = typeArgumentProto.type(typeTable) ?: return ConeKotlinErrorType("No type recorded")

        val coneType = type(type)
        return coneType.toTypeProjection(variance)
    }

}