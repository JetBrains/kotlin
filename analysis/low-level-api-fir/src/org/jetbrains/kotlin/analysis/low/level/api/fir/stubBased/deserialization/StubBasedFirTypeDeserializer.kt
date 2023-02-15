/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.builtins.StandardNames
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
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.types.Variance

class StubBasedFirTypeDeserializer(
    val moduleData: FirModuleData,
    val annotationDeserializer: StubBasedAbstractAnnotationDeserializer,
    owner: KtTypeParameterListOwner,
    val parent: StubBasedFirTypeDeserializer?,
    val containingSymbol: FirBasedSymbol<*>?
) {
    private val typeParameterNames: Map<String, FirTypeParameterSymbol>

    val ownTypeParameters: List<FirTypeParameterSymbol>
        get() = typeParameterNames.values.toList()

    init {
        val typeParameters = owner.typeParameters
        if (typeParameters.isNotEmpty()) {
            typeParameterNames = mutableMapOf()
            val builders = mutableListOf<FirTypeParameterBuilder>()
            for (typeParameter in typeParameters) {
                val name = typeParameter.nameAsSafeName
                val symbol = FirTypeParameterSymbol().also {
                    typeParameterNames[name.asString()] = it
                }
                builders += FirTypeParameterBuilder().apply {
                    moduleData = this@StubBasedFirTypeDeserializer.moduleData
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    origin = FirDeclarationOrigin.Library
                    this.name = name
                    this.symbol = symbol
                    this.containingDeclarationSymbol = containingSymbol ?: error("Top-level type parameter ???")
                    variance = typeParameter.variance
                    isReified = typeParameter.hasModifier(KtTokens.REIFIED_KEYWORD)
                    annotations += annotationDeserializer.loadTypeParameterAnnotations(typeParameter)
                }
            }

            for ((index, typeParameter) in typeParameters.withIndex()) {
                val builder = builders[index]
                builder.apply {
                    typeParameter.extendsBound?.let { bounds.add(typeRef(it)) }
                    owner.typeConstraints
                        .filter { it.subjectTypeParameterName?.getReferencedNameAsName() == typeParameter.nameAsName }
                        .forEach { typeConstraint -> typeConstraint.boundTypeReference?.let { bounds += typeRef(it) } }
                    addDefaultBoundIfNecessary()
                }.build()
            }
        } else {
            typeParameterNames = emptyMap()
        }
    }

    private fun computeClassifier(classId: ClassId?): ConeClassLikeLookupTag? {
        return classId?.toLookupTag()
    }

    fun typeRef(typeReference: KtTypeReference): FirTypeRef {
        return buildResolvedTypeRef {
            annotations += annotationDeserializer.loadTypeAnnotations(typeReference)
            type = type(typeReference, annotations.computeTypeAttributes(moduleData.session))
        }
    }

    private fun attributesFromAnnotations(typeReference: KtTypeReference): ConeAttributes =
        annotationDeserializer.loadTypeAnnotations(typeReference).computeTypeAttributes(moduleData.session)

    fun type(typeReference: KtTypeReference): ConeKotlinType {
        return type(typeReference, attributesFromAnnotations(typeReference))
    }

    fun simpleType(typeReference: KtTypeReference): ConeSimpleKotlinType {
        return simpleType(typeReference, attributesFromAnnotations(typeReference)) ?: ConeErrorType(
            ConeSimpleDiagnostic(
                "?!id:0",
                DiagnosticKind.DeserializationError
            )
        )
    }

    private fun type(typeReference: KtTypeReference, attributes: ConeAttributes): ConeKotlinType {
//todo flexible types
        //        if (typeReference.hasFlexibleTypeCapabilitiesId()) {
//            val lowerBound = simpleType(typeReference, attributes)
//            val upperBound = simpleType(typeReference.flexibleUpperBound(typeTable)!!, attributes)
//
//            val isDynamic = lowerBound == moduleData.session.builtinTypes.nothingType.coneType &&
//                    upperBound == moduleData.session.builtinTypes.nullableAnyType.coneType
//
//            return if (isDynamic) {
//                ConeDynamicType.create(moduleData.session)
//            } else {
//                ConeFlexibleType(lowerBound!!, upperBound!!)
//            }
//        }

        return simpleType(typeReference, attributes) ?: ConeErrorType(ConeSimpleDiagnostic("?!id:0", DiagnosticKind.DeserializationError))
    }

    private fun typeParameterSymbol(typeParameterName: String): ConeTypeParameterLookupTag? =
        typeParameterNames[typeParameterName]?.toLookupTag() ?: parent?.typeParameterSymbol(typeParameterName)

    fun FirClassLikeSymbol<*>.typeParameters(): List<FirTypeParameterSymbol> =
        (fir as? FirTypeParameterRefsOwner)?.typeParameters?.map { it.symbol }.orEmpty()

    private fun simpleType(typeReference: KtTypeReference, attributes: ConeAttributes): ConeSimpleKotlinType? {
        val constructor = typeSymbol(typeReference) ?: return null
        val isNullable = typeReference.typeElement is KtNullableType
        if (constructor is ConeTypeParameterLookupTag) {
            return ConeTypeParameterTypeImpl(constructor, isNullable = isNullable).let {
//                todo test intersection with Any
                //                if (Flags.DEFINITELY_NOT_NULL_TYPE.get(typeReference.flags))
//                    ConeDefinitelyNotNullType.create(it, moduleData.session.typeContext, avoidComprehensiveCheck = true) ?: it
//                else
                it
            }
        }
        if (constructor !is ConeClassLikeLookupTag) return null

        fun KtUserType.collectAllArguments(): List<KtTypeProjection> =
            typeArguments// + outerType(typeTable)?.collectAllArguments().orEmpty()

        val typeElement = typeReference.typeElement?.unwrapNullability()
        val arguments = when (typeElement) {
            is KtUserType -> typeElement.collectAllArguments().map { typeArgument(it) }.toTypedArray()
            is KtFunctionType -> buildList {
                typeElement.receiver?.let { add(type(it.typeReference).toTypeProjection(Variance.INVARIANT)) }
                addAll(typeElement.parameters.map { type(it.typeReference!!).toTypeProjection(Variance.INVARIANT) })
                add(type(typeElement.returnTypeReference!!).toTypeProjection(Variance.INVARIANT))
            }.toTypedArray()
            else -> error("not supported $typeElement")
        }

        val simpleType = if (typeReference.getAllModifierLists().any { it.hasSuspendModifier() }) {
            createSuspendFunctionType(constructor, arguments, isNullable = isNullable, attributes)
        } else {
            ConeClassLikeTypeImpl(
                constructor,
                arguments,
                isNullable = isNullable,
                if (typeElement is KtFunctionType && typeElement.receiver != null) ConeAttributes.WithExtensionFunctionType else attributes
            )
        }
        return simpleType
//        val abbreviatedTypeProto = typeReference.abbreviatedType(typeTable) ?: return simpleType
//        return simpleType(abbreviatedTypeProto, attributes)
    }

    private fun KtElementImplStub<*>.getAllModifierLists(): Array<out KtDeclarationModifierList> =
        getStubOrPsiChildren(KtStubElementTypes.MODIFIER_LIST, KtStubElementTypes.MODIFIER_LIST.arrayFactory)

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
            isNullable,
            attributes
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
                            ClassId(kind.packageFqName, kind.numberedClassName(arity)).toLookupTag(), arguments, isNullable, attributes
                        )
                    } else {
                        null
                    }
                }
                else -> null
            }
        return result ?: ConeErrorType(
            ConeSimpleDiagnostic(
                "Bad suspend function in metadata with constructor: $functionTypeConstructor", DiagnosticKind.DeserializationError
            )
        )
    }

    private fun typeSymbol(typeReference: KtTypeReference): ConeClassifierLookupTag? {
        val typeElement = typeReference.typeElement?.unwrapNullability()
        if (typeElement is KtFunctionType) {
            val arity = (if (typeElement.receiver != null) 1 else 0) + typeElement.parameters.size
            val isSuspend = typeReference.getAllModifierLists().any { it.hasSuspendModifier() }
            val functionClassId = if (isSuspend) StandardNames.getSuspendFunctionClassId(arity) else StandardNames.getFunctionClassId(arity)
            return computeClassifier(functionClassId)
        }
        val type = typeElement as KtUserType
        val stub = type.stub!!
        return when {
            !stub.onTypeParameter() -> computeClassifier(stub.classId())//todo avoid full classId in the index, probably combine with type-parameter byte
            else -> typeParameterSymbol(type.referencedName!!)
        }
    }


    private fun typeArgument(projection: KtTypeProjection): ConeTypeProjection {
        if (projection.projectionKind == KtProjectionKind.STAR) {
            return ConeStarProjection
        }

        val variance = when (projection.projectionKind) {
            KtProjectionKind.IN -> Variance.IN_VARIANCE
            KtProjectionKind.OUT -> Variance.OUT_VARIANCE
            KtProjectionKind.NONE -> Variance.INVARIANT
            KtProjectionKind.STAR -> throw AssertionError("* should not be here")
        }
        val type = type(projection.typeReference!!)
        return type.toTypeProjection(variance)
    }
}