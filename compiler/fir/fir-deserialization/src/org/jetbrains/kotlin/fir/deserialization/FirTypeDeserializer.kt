/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.ClassId
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
    private val typeParameterDescriptors: Map<Int, FirTypeParameterSymbol> = if (typeParameterProtos.isNotEmpty()) {
        LinkedHashMap<Int, FirTypeParameterSymbol>()
    } else {
        mapOf()
    }

    private val typeParameterNames: Map<String, FirTypeParameterSymbol>

    val ownTypeParameters: List<FirTypeParameterSymbol>
        get() = typeParameterDescriptors.values.toList()

    init {
        if (typeParameterProtos.isNotEmpty()) {
            typeParameterNames = mutableMapOf()
            val result = typeParameterDescriptors as LinkedHashMap<Int, FirTypeParameterSymbol>
            val builders = mutableListOf<FirTypeParameterBuilder>()
            for (proto in typeParameterProtos) {
                if (!proto.hasId()) continue
                val name = nameResolver.getName(proto.name)
                val symbol = FirTypeParameterSymbol().also {
                    typeParameterNames[name.asString()] = it
                }
                builders += FirTypeParameterBuilder().apply {
                    session = this@FirTypeDeserializer.session
                    origin = FirDeclarationOrigin.Library
                    this.name = name
                    this.symbol = symbol
                    variance = proto.variance.convertVariance()
                    isReified = proto.reified
                }
                result[proto.id] = symbol
            }

            for ((index, proto) in typeParameterProtos.withIndex()) {
                val builder = builders[index]
                builder.apply {
                    proto.upperBoundList.mapTo(bounds) {
                        buildResolvedTypeRef { type = type(it, ConeAttributes.Empty) }
                    }
                    addDefaultBoundIfNecessary()
                }.build()
            }
        } else {
            typeParameterNames = emptyMap()
        }
    }

    private fun computeClassifier(fqNameIndex: Int): ConeClassLikeLookupTag? {
        try {
            val id = nameResolver.getClassId(fqNameIndex)
            return ConeClassLikeLookupTagImpl(id)
        } catch (e: Throwable) {
            throw RuntimeException("Looking up for ${nameResolver.getClassId(fqNameIndex)}", e)
        }
    }

    fun type(proto: ProtoBuf.Type, attributes: ConeAttributes): ConeKotlinType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val lowerBound = simpleType(proto, attributes)
            val upperBound = simpleType(proto.flexibleUpperBound(typeTable)!!, attributes)
            return ConeFlexibleType(lowerBound!!, upperBound!!)
            //c.components.flexibleTypeDeserializer.create(proto, id, lowerBound, upperBound)
        }

        return simpleType(proto, attributes) ?: ConeKotlinErrorType(ConeSimpleDiagnostic("?!id:0", DiagnosticKind.DeserializationError))
    }

    private fun typeParameterSymbol(typeParameterId: Int): ConeTypeParameterLookupTag? =
        typeParameterDescriptors[typeParameterId]?.toLookupTag() ?: parent?.typeParameterSymbol(typeParameterId)


    private fun ProtoBuf.TypeParameter.Variance.convertVariance(): Variance {
        return when (this) {
            ProtoBuf.TypeParameter.Variance.IN -> Variance.IN_VARIANCE
            ProtoBuf.TypeParameter.Variance.OUT -> Variance.OUT_VARIANCE
            ProtoBuf.TypeParameter.Variance.INV -> Variance.INVARIANT
        }
    }


    fun FirClassLikeSymbol<*>.typeParameters(): List<FirTypeParameterSymbol> =
        (fir as? FirTypeParameterRefsOwner)?.typeParameters?.map { it.symbol }.orEmpty()

    fun simpleType(proto: ProtoBuf.Type, attributes: ConeAttributes): ConeLookupTagBasedType? {

        val constructor = typeSymbol(proto) ?: return null
        if (constructor is ConeTypeParameterLookupTag) return ConeTypeParameterTypeImpl(constructor, isNullable = proto.nullable)
        if (constructor !is ConeClassLikeLookupTag) return null

        fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
            argumentList + outerType(typeTable)?.collectAllArguments().orEmpty()

        val arguments = proto.collectAllArguments().map(this::typeArgument).toTypedArray()

        val simpleType = if (Flags.SUSPEND_TYPE.get(proto.flags)) {
            createSuspendFunctionType(constructor, arguments, isNullable = proto.nullable, attributes)
        } else {
            ConeClassLikeTypeImpl(constructor, arguments, isNullable = proto.nullable, attributes)
        }

        val abbreviatedTypeProto = proto.abbreviatedType(typeTable) ?: return simpleType

        return simpleType(abbreviatedTypeProto, attributes)

    }

    private fun createSuspendFunctionTypeForBasicCase(
        //annotations: Annotations, TODO?,
        functionTypeConstructor: ConeClassLikeLookupTag,
        arguments: Array<ConeTypeProjection>,
        isNullable: Boolean,
        attributes: ConeAttributes
    ): ConeClassLikeType? {
        fun ConeClassLikeType.isContinuation(): Boolean {
            if (this.typeArguments.size != 1) return false
            if (this.lookupTag.classId != CONTINUATION_INTERFACE_CLASS_ID) return false
            return true
        }

        val returnType = arguments.lastOrNull()
        val continuationType = arguments.getOrNull(arguments.lastIndex - 1) as? ConeClassLikeType ?: return null

        if (!continuationType.isContinuation()) return ConeClassLikeTypeImpl(functionTypeConstructor, arguments, isNullable, attributes)

        val suspendReturnType = continuationType.typeArguments.single() as ConeKotlinTypeProjection

        val valueParameters = arguments.dropLast(2)

        val kind = FunctionClassKind.SuspendFunction
        return ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(ClassId(kind.packageFqName, kind.numberedClassName(valueParameters.size))),
            (valueParameters + suspendReturnType).toTypedArray(),
            isNullable, attributes
        )
    }

    private fun createSuspendFunctionType(
        //annotations: Annotations, TODO?
        functionTypeConstructor: ConeClassLikeLookupTag,
        arguments: Array<ConeTypeProjection>,
        isNullable: Boolean,
        attributes: ConeAttributes
    ): ConeClassLikeType {
        val result =
            when (functionTypeConstructor.toSymbol(session)!!.firUnsafe<FirTypeParameterRefsOwner>().typeParameters.size - arguments.size) {
                0 -> createSuspendFunctionTypeForBasicCase(/* annotations, */ functionTypeConstructor, arguments, isNullable, attributes)
//                 This case for types written by eap compiler 1.1
                1 -> {
                    val arity = arguments.size - 1
                    if (arity >= 0) {
                        val kind = FunctionClassKind.SuspendFunction
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(ClassId(kind.packageFqName, kind.numberedClassName(arity))),
                            arguments,
                            isNullable,
                            attributes
                        )
                    } else {
                        null
                    }
                }
                else -> null
            }
        return result ?: ConeClassErrorType(
            ConeSimpleDiagnostic(
                "Bad suspend function in metadata with constructor: $functionTypeConstructor",
                DiagnosticKind.DeserializationError
            )
        )
    }

    private fun typeSymbol(proto: ProtoBuf.Type): ConeClassifierLookupTag? {

        return when {
            proto.hasClassName() -> computeClassifier(proto.className)
            proto.hasTypeAliasName() -> computeClassifier(proto.typeAliasName)
            proto.hasTypeParameter() -> typeParameterSymbol(proto.typeParameter)
            proto.hasTypeParameterName() -> {
                val name = nameResolver.getString(proto.typeParameterName)
                typeParameterNames[name]?.toLookupTag()
            }
            else -> null
        }
    }


    private fun typeArgument(typeArgumentProto: ProtoBuf.Type.Argument): ConeTypeProjection {
        if (typeArgumentProto.projection == ProtoBuf.Type.Argument.Projection.STAR) {
            return ConeStarProjection
        }

        val variance = ProtoEnumFlags.variance(typeArgumentProto.projection)
        val type = typeArgumentProto.type(typeTable)
            ?: return ConeKotlinErrorType(ConeSimpleDiagnostic("No type recorded", DiagnosticKind.DeserializationError))

        // TODO: check that here we don't have any attributes
        val coneType = type(type, ConeAttributes.Empty)
        return coneType.toTypeProjection(variance)
    }

}
