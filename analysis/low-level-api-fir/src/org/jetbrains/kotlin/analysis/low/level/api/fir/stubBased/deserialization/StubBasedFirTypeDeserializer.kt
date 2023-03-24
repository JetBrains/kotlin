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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl
import org.jetbrains.kotlin.types.Variance

class StubBasedFirTypeDeserializer(
    val moduleData: FirModuleData,
    val annotationDeserializer: StubBasedAnnotationDeserializer,
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
            typeParameterNames = emptyMap()
        }
    }

    private fun computeClassifier(classId: ClassId?): ConeClassLikeLookupTag? {
        return classId?.toLookupTag()
    }

    fun typeRef(typeReference: KtTypeReference): FirTypeRef {
        return buildResolvedTypeRef {
            source = KtRealPsiSourceElement(typeReference)
            annotations += annotationDeserializer.loadAnnotations(typeReference)
            type = type(typeReference, annotations.computeTypeAttributes(moduleData.session))
        }
    }

    private fun attributesFromAnnotations(typeReference: KtTypeReference): ConeAttributes =
        annotationDeserializer.loadAnnotations(typeReference).computeTypeAttributes(moduleData.session)

    fun type(typeReference: KtTypeReference): ConeKotlinType {
        return type(typeReference, attributesFromAnnotations(typeReference))
    }

    private fun type(typeReference: KtTypeReference, attributes: ConeAttributes): ConeKotlinType {
        val typeElement = typeReference.typeElement
        if (typeElement is KtDynamicType) {
            val stubs = typeElement.stub?.childrenStubs
            if (stubs?.size == 2) {
                val lowerBound = simpleType(attributes, stubs[0].psi as KtTypeElement, typeReference)
                val upperBound = simpleType(attributes, stubs[1].psi as KtTypeElement, typeReference)

                val isDynamic = lowerBound == moduleData.session.builtinTypes.nothingType.coneType &&
                        upperBound == moduleData.session.builtinTypes.nullableAnyType.coneType

                return if (isDynamic) {
                    ConeDynamicType.create(moduleData.session)
                } else {
                    ConeFlexibleType(lowerBound!!, upperBound!!)
                }
            }
        }

        return simpleType(attributes, typeReference.typeElement, typeReference) ?: ConeErrorType(ConeSimpleDiagnostic("?!id:0", DiagnosticKind.DeserializationError))
    }

    private fun typeParameterSymbol(typeParameterName: String): ConeTypeParameterLookupTag? =
        typeParameterNames[typeParameterName]?.toLookupTag() ?: parent?.typeParameterSymbol(typeParameterName)

    fun FirClassLikeSymbol<*>.typeParameters(): List<FirTypeParameterSymbol> =
        (fir as? FirTypeParameterRefsOwner)?.typeParameters?.map { it.symbol }.orEmpty()

    private fun simpleType(attributes: ConeAttributes, typeElement: KtTypeElement?, typeReference: KtTypeReference): ConeSimpleKotlinType? {
        val constructor = typeSymbol(typeElement, typeReference) ?: return null
        val isNullable = typeElement is KtNullableType
        if (constructor is ConeTypeParameterLookupTag) {
            return ConeTypeParameterTypeImpl(constructor, isNullable = isNullable)
        }
        if (constructor !is ConeClassLikeLookupTag) return null

        val unwrappedTypeElement = typeElement?.unwrapNullability()
        val arguments = when (unwrappedTypeElement) {
            is KtUserType -> unwrappedTypeElement.typeArguments.map { typeArgument(it) }.toTypedArray()
            is KtFunctionType -> buildList {
                unwrappedTypeElement.receiver?.let { add(type(it.typeReference).toTypeProjection(Variance.INVARIANT)) }
                addAll(unwrappedTypeElement.parameters.map { type(it.typeReference!!).toTypeProjection(Variance.INVARIANT) })
                add(type(unwrappedTypeElement.returnTypeReference!!).toTypeProjection(Variance.INVARIANT))
            }.toTypedArray()
            else -> error("not supported $unwrappedTypeElement")
        }

        return ConeClassLikeTypeImpl(
            constructor,
            arguments,
            isNullable = isNullable,
            if (unwrappedTypeElement is KtFunctionType && unwrappedTypeElement.receiver != null) ConeAttributes.WithExtensionFunctionType else attributes
        )
    }

    private fun KtElementImplStub<*>.getAllModifierLists(): Array<out KtDeclarationModifierList> =
        getStubOrPsiChildren(KtStubElementTypes.MODIFIER_LIST, KtStubElementTypes.MODIFIER_LIST.arrayFactory)

    private fun typeSymbol(typeElement: KtTypeElement?, typeReference: KtTypeReference): ConeClassifierLookupTag? {
        val typeElementUnwrapped = typeElement?.unwrapNullability()
        if (typeElementUnwrapped is KtFunctionType) {
            val arity = (if (typeElementUnwrapped.receiver != null) 1 else 0) + typeElementUnwrapped.parameters.size
            val isSuspend = typeReference.getAllModifierLists().any { it.hasSuspendModifier() }
            val functionClassId = if (isSuspend) StandardNames.getSuspendFunctionClassId(arity) else StandardNames.getFunctionClassId(arity)
            return computeClassifier(functionClassId)
        }
        val type = typeElementUnwrapped as KtUserType
        val referencedName = type.referencedName
        return typeParameterSymbol(referencedName!!) ?: computeClassifier(type.classId())
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

internal fun KtUserType.classId(): ClassId {
    val packageFragments = mutableListOf<String>()
    val classFragments = mutableListOf<String>()

    fun collectFragments(type: KtUserType) {
        val userType = type.getStubOrPsiChild(KtStubElementTypes.USER_TYPE)
        if (userType != null) {
            collectFragments(userType)
        }
        val referenceExpression = userType?.referenceExpression as? KtNameReferenceExpression
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
    referencedName?.let { classFragments.add(it) }
    return ClassId(FqName.fromSegments(packageFragments),
        FqName.fromSegments(classFragments),
        false
    )
}