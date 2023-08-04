/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.computeTypeAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.declarations.utils.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.shouldIjPlatformExceptionBeRethrown

class FirTypeDeserializer(
    private val moduleData: FirModuleData,
    private val nameResolver: NameResolver,
    private val typeTable: TypeTable,
    private val annotationDeserializer: AbstractAnnotationDeserializer,
    typeParameterProtos: List<ProtoBuf.TypeParameter>,
    private val parent: FirTypeDeserializer?,
    private val containingSymbol: FirBasedSymbol<*>?
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
                    moduleData = this@FirTypeDeserializer.moduleData
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    origin = FirDeclarationOrigin.Library
                    this.name = name
                    this.symbol = symbol
                    this.containingDeclarationSymbol = containingSymbol ?: error("Top-level type parameter ???")
                    variance = proto.variance.convertVariance()
                    isReified = proto.reified
                    annotations += annotationDeserializer.loadTypeParameterAnnotations(proto, nameResolver)
                }
                result[proto.id] = symbol
            }

            for ((index, proto) in typeParameterProtos.withIndex()) {
                val builder = builders[index]
                builder.apply {
                    proto.upperBounds(typeTable).mapTo(bounds) {
                        buildResolvedTypeRef { type = type(it) }
                    }
                    addDefaultBoundIfNecessary()
                }.build()
            }
        } else {
            typeParameterNames = emptyMap()
        }
    }

    private fun computeClassifier(fqNameIndex: Int): ConeClassLikeLookupTag {
        try {
            // We can't just load local types as is, because later we will get an exception
            // while trying to get corresponding FIR class
            val id = nameResolver.getClassId(fqNameIndex).takeIf { !it.isLocal } ?: StandardClassIds.Any
            return id.toLookupTag()
        } catch (e: Throwable) {
            if (shouldIjPlatformExceptionBeRethrown(e)) throw e
            throw RuntimeException("Looking up for ${nameResolver.getClassId(fqNameIndex)}", e)
        }
    }

    fun typeRef(proto: ProtoBuf.Type): FirResolvedTypeRef {
        return buildResolvedTypeRef {
            annotations += annotationDeserializer.loadTypeAnnotations(proto, nameResolver)
            type = type(proto, annotations.computeTypeAttributes(moduleData.session, shouldExpandTypeAliases = false))
        }
    }

    private fun attributesFromAnnotations(proto: ProtoBuf.Type): ConeAttributes =
        annotationDeserializer.loadTypeAnnotations(proto, nameResolver)
            .computeTypeAttributes(moduleData.session, shouldExpandTypeAliases = false)

    fun type(proto: ProtoBuf.Type): ConeKotlinType {
        return type(proto, attributesFromAnnotations(proto))
    }

    fun simpleType(proto: ProtoBuf.Type): ConeSimpleKotlinType {
        return simpleType(proto, attributesFromAnnotations(proto))
            ?: ConeErrorType(ConeSimpleDiagnostic("?!id:0", DiagnosticKind.DeserializationError))
    }

    private fun type(proto: ProtoBuf.Type, attributes: ConeAttributes): ConeKotlinType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val lowerBound = simpleType(proto, attributes)
            val upperBound = simpleType(proto.flexibleUpperBound(typeTable)!!, attributes)

            val isDynamic = lowerBound == moduleData.session.builtinTypes.nothingType.coneType &&
                    upperBound == moduleData.session.builtinTypes.nullableAnyType.coneType

            return if (isDynamic) {
                ConeDynamicType.create(moduleData.session)
            } else {
                ConeFlexibleType(lowerBound!!, upperBound!!)
            }
        }

        return simpleType(proto, attributes) ?: ConeErrorType(ConeSimpleDiagnostic("?!id:0", DiagnosticKind.DeserializationError))
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

    private fun simpleType(proto: ProtoBuf.Type, attributes: ConeAttributes): ConeSimpleKotlinType? {
        val constructor = typeSymbol(proto) ?: return null
        if (constructor is ConeTypeParameterLookupTag) {
            return ConeTypeParameterTypeImpl(constructor, isNullable = proto.nullable).let {
                if (Flags.DEFINITELY_NOT_NULL_TYPE.get(proto.flags))
                    ConeDefinitelyNotNullType.create(it, moduleData.session.typeContext, avoidComprehensiveCheck = true) ?: it
                else
                    it
            }
        }
        if (constructor !is ConeClassLikeLookupTag) return null

        fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
            argumentList + outerType(typeTable)?.collectAllArguments().orEmpty()

        val arguments = proto.collectAllArguments().map(this::typeArgument).toTypedArray()

        val extensionFunctionalKind = moduleData.session.functionTypeService.extractSingleExtensionKindForDeserializedConeType(
            constructor.classId, attributes.customAnnotations
        )

        val simpleType = when {
            extensionFunctionalKind != null -> {
                val newConstructor = if (arguments.isNotEmpty()) {
                    ConeClassLikeLookupTagImpl(extensionFunctionalKind.numberedClassId(arguments.size - 1))
                } else {
                    return ConeErrorType(
                        ConeSimpleDiagnostic("Illegal number of arguments for extension functional type $extensionFunctionalKind"),
                        typeArguments = arguments,
                        attributes = attributes
                    )
                }
                ConeClassLikeTypeImpl(newConstructor, arguments, isNullable = proto.nullable, attributes)
            }
            Flags.SUSPEND_TYPE.get(proto.flags) -> {
                createSuspendFunctionType(constructor, arguments, isNullable = proto.nullable, attributes)
            }
            else -> ConeClassLikeTypeImpl(constructor, arguments, isNullable = proto.nullable, attributes)
        }

        // TODO: Return abbreviated types for type aliases, see KT-58542
        return simpleType
    }

    private fun createSuspendFunctionTypeForBasicCase(
        functionTypeConstructor: ConeClassLikeLookupTag,
        arguments: Array<ConeTypeProjection>,
        isNullable: Boolean,
        attributes: ConeAttributes
    ): ConeClassLikeType? {
        fun ConeClassLikeType.isContinuation(): Boolean {
            if (this.typeArguments.size != 1) return false
            if (this.lookupTag.classId != StandardClassIds.Continuation) return false
            return true
        }

        val continuationType = arguments.getOrNull(arguments.lastIndex - 1) as? ConeClassLikeType ?: return null
        if (!continuationType.isContinuation()) return ConeClassLikeTypeImpl(functionTypeConstructor, arguments, isNullable, attributes)
        val suspendReturnType = continuationType.typeArguments.single() as ConeKotlinTypeProjection
        val valueParameters = arguments.dropLast(2)

        val kind = FunctionTypeKind.SuspendFunction
        return ConeClassLikeTypeImpl(
            ClassId(kind.packageFqName, kind.numberedClassName(valueParameters.size)).toLookupTag(),
            (valueParameters + suspendReturnType).toTypedArray(),
            isNullable, attributes
        )
    }

    private fun createSuspendFunctionType(
        functionTypeConstructor: ConeClassLikeLookupTag,
        arguments: Array<ConeTypeProjection>,
        isNullable: Boolean,
        attributes: ConeAttributes
    ): ConeClassLikeType {
        val result =
            when ((functionTypeConstructor.toSymbol(moduleData.session)?.fir as FirTypeParameterRefsOwner).typeParameters.size - arguments.size) {
                0 -> createSuspendFunctionTypeForBasicCase(functionTypeConstructor, arguments, isNullable, attributes)
                1 -> {
                    val arity = arguments.size - 1
                    if (arity >= 0) {
                        val kind = FunctionTypeKind.SuspendFunction
                        ConeClassLikeTypeImpl(
                            ClassId(kind.packageFqName, kind.numberedClassName(arity)).toLookupTag(),
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
        return result ?: ConeErrorType(
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
            ?: return ConeErrorType(ConeSimpleDiagnostic("No type recorded", DiagnosticKind.DeserializationError))
        val coneType = type(type)
        return coneType.toTypeProjection(variance)
    }
}
