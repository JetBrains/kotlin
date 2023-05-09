/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.computeTypeAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.declarations.utils.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance

class StubBasedFirTypeDeserializer(
    private val moduleData: FirModuleData,
    private val annotationDeserializer: StubBasedAnnotationDeserializer,
    private val parent: StubBasedFirTypeDeserializer?,
    private val containingSymbol: FirBasedSymbol<*>?,
    owner: KtTypeParameterListOwner
) {
    private val typeParametersByName: Map<String, FirTypeParameterSymbol>

    val ownTypeParameters: List<FirTypeParameterSymbol>
        get() = typeParametersByName.values.toList()

    init {
        val typeParameters = owner.typeParameters
        if (typeParameters.isNotEmpty()) {
            typeParametersByName = mutableMapOf()
            val builders = mutableListOf<FirTypeParameterBuilder>()
            for (typeParameter in typeParameters) {
                val name = typeParameter.nameAsSafeName
                val symbol = FirTypeParameterSymbol().also {
                    typeParametersByName[name.asString()] = it
                }
                builders += FirTypeParameterBuilder().apply {
                    source = KtRealPsiSourceElement(typeParameter)
                    moduleData = this@StubBasedFirTypeDeserializer.moduleData
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    origin = FirDeclarationOrigin.Library
                    this.name = name
                    this.symbol = symbol
                    this.containingDeclarationSymbol = containingSymbol ?: error("Top-level type parameter ???")
                    variance = typeParameter.variance
                    isReified = typeParameter.hasModifier(KtTokens.REIFIED_KEYWORD)
                    annotations += annotationDeserializer.loadAnnotations(typeParameter)
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
            typeParametersByName = emptyMap()
        }
    }

    private fun computeClassifier(classId: ClassId?): ConeClassLikeLookupTag? {
        return classId?.toLookupTag()
    }

    fun typeRef(typeReference: KtTypeReference): FirTypeRef {
        return buildResolvedTypeRef {
            source = KtRealPsiSourceElement(typeReference)
            annotations += annotationDeserializer.loadAnnotations(typeReference)
            type = type(typeReference, annotations.computeTypeAttributes(moduleData.session, shouldExpandTypeAliases = false))
        }
    }

    fun type(typeReference: KtTypeReference): ConeKotlinType {
        val annotations = annotationDeserializer.loadAnnotations(typeReference).toMutableList()
        val parent = typeReference.stub?.parentStub
        if (parent is KotlinParameterStubImpl) {
            (parent as? KotlinParameterStubImpl)?.functionTypeParameterName?.let { paramName ->
                annotations += buildAnnotation {
                    annotationTypeRef = buildResolvedTypeRef {
                        type = StandardNames.FqNames.parameterNameClassId.toLookupTag()
                            .constructClassType(emptyArray(), isNullable = false)
                    }
                    this.argumentMapping = buildAnnotationArgumentMapping {
                        mapping[Name.identifier("name")] =
                            buildConstExpression(null, ConstantValueKind.String, paramName, setType = true)
                    }
                }
            }
        }
        return type(typeReference, annotations.computeTypeAttributes(moduleData.session, shouldExpandTypeAliases = false))
    }

    fun type(type: KotlinTypeBean): ConeKotlinType? {
        when (type) {
            is KotlinTypeParameterTypeBean -> {
                val lookupTag =
                    typeParametersByName[type.typeParameterName]?.toLookupTag() ?: parent?.typeParameterSymbol(type.typeParameterName)
                    ?: return null
                return ConeTypeParameterTypeImpl(lookupTag, isNullable = type.nullable).let {
                    if (type.definitelyNotNull)
                        ConeDefinitelyNotNullType.create(it, moduleData.session.typeContext, avoidComprehensiveCheck = true) ?: it
                    else
                        it
                }
            }
            is KotlinClassTypeBean -> {
                val projections = type.arguments.map { typeArgumentBean ->
                    val kind = typeArgumentBean.projectionKind
                    if (kind == KtProjectionKind.STAR) {
                        return@map ConeStarProjection
                    }
                    val argBean = typeArgumentBean.type!!
                    val lowerBound = type(argBean) ?: error("Broken type argument ${typeArgumentBean.type}")
                    typeArgument(lowerBound, kind)
                }
                return ConeClassLikeTypeImpl(
                    type.classId.toLookupTag(),
                    projections.toTypedArray(),
                    isNullable = type.nullable,
                    ConeAttributes.Empty
                )
            }
            is KotlinFlexibleTypeBean -> {
                val lowerBound = type(type.lowerBound)
                val upperBound = type(type.upperBound)
                return ConeFlexibleType(
                    lowerBound as? ConeSimpleKotlinType ?: error("Unexpected lower bound $lowerBound"),
                    upperBound as? ConeSimpleKotlinType ?: error("Unexpected upper bound $upperBound")
                )
            }
        }
    }

    private fun type(typeReference: KtTypeReference, attributes: ConeAttributes): ConeKotlinType {
        val upperBoundType = ((typeReference.typeElement as? KtUserType)?.stub as? KotlinUserTypeStubImpl)?.upperBound
        if (upperBoundType != null) {
            val lowerBound = simpleType(typeReference, attributes)
            val upperBound = type(upperBoundType)

            val isDynamic = lowerBound == moduleData.session.builtinTypes.nothingType.coneType &&
                    upperBound == moduleData.session.builtinTypes.nullableAnyType.coneType

            return if (isDynamic) {
                ConeDynamicType.create(moduleData.session)
            } else {
                ConeFlexibleType(lowerBound!!, upperBound as ConeSimpleKotlinType)
            }
        }

        return simpleType(typeReference, attributes) ?: ConeErrorType(ConeSimpleDiagnostic("?!id:0", DiagnosticKind.DeserializationError))
    }

    private fun typeParameterSymbol(typeParameterName: String): ConeTypeParameterLookupTag? =
        typeParametersByName[typeParameterName]?.toLookupTag() ?: parent?.typeParameterSymbol(typeParameterName)

    fun FirClassLikeSymbol<*>.typeParameters(): List<FirTypeParameterSymbol> =
        (fir as? FirTypeParameterRefsOwner)?.typeParameters?.map { it.symbol }.orEmpty()

    private fun simpleType(typeReference: KtTypeReference, attributes: ConeAttributes): ConeSimpleKotlinType? {
        val constructor = typeSymbol(typeReference) ?: return null
        val isNullable = typeReference.typeElement is KtNullableType
        if (constructor is ConeTypeParameterLookupTag) {
            return ConeTypeParameterTypeImpl(constructor, isNullable = isNullable).let {
                if (typeReference.typeElement?.unwrapNullability() is KtIntersectionType) {
                    ConeDefinitelyNotNullType.create(it, moduleData.session.typeContext, avoidComprehensiveCheck = true) ?: it
                } else it
            }
        }
        if (constructor !is ConeClassLikeLookupTag) return null

        val typeElement = typeReference.typeElement?.unwrapNullability()
        val arguments = when (typeElement) {
            is KtUserType -> typeElement.typeArguments.map { typeArgument(it) }.toTypedArray()
            is KtFunctionType -> buildList {
                typeElement.receiver?.let { add(type(it.typeReference).toTypeProjection(Variance.INVARIANT)) }
                addAll(typeElement.parameters.map { type(it.typeReference!!).toTypeProjection(Variance.INVARIANT) })
                add(type(typeElement.returnTypeReference!!).toTypeProjection(Variance.INVARIANT))
            }.toTypedArray()
            else -> error("not supported $typeElement")
        }

        return ConeClassLikeTypeImpl(
            constructor,
            arguments,
            isNullable = isNullable,
            if (typeElement is KtFunctionType && typeElement.receiver != null) ConeAttributes.WithExtensionFunctionType else attributes
        )
    }

    private fun KtElementImplStub<*>.getAllModifierLists(): Array<out KtDeclarationModifierList> =
        getStubOrPsiChildren(KtStubElementTypes.MODIFIER_LIST, KtStubElementTypes.MODIFIER_LIST.arrayFactory)

    private fun typeSymbol(typeReference: KtTypeReference): ConeClassifierLookupTag? {
        val typeElement = typeReference.typeElement?.unwrapNullability()
        if (typeElement is KtFunctionType) {
            val arity = (if (typeElement.receiver != null) 1 else 0) + typeElement.parameters.size
            val isSuspend = typeReference.getAllModifierLists().any { it.hasSuspendModifier() }
            val functionClassId = if (isSuspend) StandardNames.getSuspendFunctionClassId(arity) else StandardNames.getFunctionClassId(arity)
            return computeClassifier(functionClassId)
        }
        if (typeElement is KtIntersectionType) {
            val leftTypeRef = typeElement.getLeftTypeRef() ?: return null
            //T&Any
            return typeSymbol(leftTypeRef)
        }
        val type = typeElement as KtUserType
        val referencedName = type.referencedName
        return typeParameterSymbol(referencedName!!) ?: computeClassifier(type.classId())
    }


    private fun typeArgument(projection: KtTypeProjection): ConeTypeProjection {
        if (projection.projectionKind == KtProjectionKind.STAR) {
            return ConeStarProjection
        }

        val type = type(projection.typeReference!!)
        return typeArgument(type, projection.projectionKind)
    }

    private fun typeArgument(
        type: ConeKotlinType,
        projectionKind: KtProjectionKind
    ): ConeTypeProjection {
        val variance = when (projectionKind) {
            KtProjectionKind.IN -> Variance.IN_VARIANCE
            KtProjectionKind.OUT -> Variance.OUT_VARIANCE
            KtProjectionKind.NONE -> Variance.INVARIANT
            KtProjectionKind.STAR -> throw AssertionError("* should not be here")
        }
        return type.toTypeProjection(variance)
    }
}

/**
 * Retrieves classId from [KtUserType] for compiled code only.
 *
 * It relies on [org.jetbrains.kotlin.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl.isClassRef],
 * which is set during cls analysis only.
 */
internal fun KtUserType.classId(): ClassId {
    val packageFragments = mutableListOf<String>()
    val classFragments = mutableListOf<String>()

    fun collectFragments(type: KtUserType) {
        val userType = type.getStubOrPsiChild(KtStubElementTypes.USER_TYPE)
        if (userType != null) {
            collectFragments(userType)
        }
        val referenceExpression = type.referenceExpression as? KtNameReferenceExpression
        if (referenceExpression != null) {
            val referencedName = referenceExpression.getReferencedName()
            val stub = referenceExpression.stub
            if (stub is KotlinNameReferenceExpressionStubImpl && stub.isClassRef) {
                classFragments.add(referencedName)
            } else {
                packageFragments.add(referencedName)
            }
        }
    }
    collectFragments(this)
    if (classFragments.isEmpty()) {
        //stub is re-built from decompiled text and additional information is already missed
        return ClassId(
            FqName.fromSegments(packageFragments).parent(),
            FqName(packageFragments.last()),
            false
        )
    }
    return ClassId(
        FqName.fromSegments(packageFragments),
        FqName.fromSegments(classFragments),
        false
    )
}