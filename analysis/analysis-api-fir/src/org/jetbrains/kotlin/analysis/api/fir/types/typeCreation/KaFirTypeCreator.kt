/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.typeCreation

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.*
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.withParameterNameAnnotation
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirTypeCreator(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseTypeCreator<KaFirSession>(), KaFirSessionComponent {
    override fun classType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        return buildClassType(KaBaseClassTypeBuilder.ByClassId(classId, this).apply(init))
    }

    override fun classType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        return buildClassType(KaBaseClassTypeBuilder.BySymbol(symbol, this).apply(init))
    }

    private fun buildClassType(builder: KaBaseClassTypeBuilder): KaType {
        val lookupTag = when (builder) {
            is KaBaseClassTypeBuilder.ByClassId -> {
                val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(builder.classId)
                    ?: return ConeErrorType(ConeUnresolvedSymbolError(builder.classId)).withAttributes(builder.annotations).asKaType()
                classSymbol.toLookupTag()
            }
            is KaBaseClassTypeBuilder.BySymbol -> {
                val symbol = builder.symbol
                symbol.classId?.toLookupTag() ?: symbol.firSymbol.toLookupTag()
            }
        }

        val typeContext = rootModuleSession.typeContext
        val coneType = typeContext.createSimpleType(
            lookupTag,
            builder.typeArguments.map { it.coneTypeProjection },
            builder.isMarkedNullable
        ) as ConeClassLikeType

        return coneType.withAttributes(builder.annotations).asKaType()
    }

    override fun typeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType =
        withValidityAssertion {
            val builder = KaBaseTypeParameterTypeBuilder.BySymbol(symbol, this).apply(init)
            val symbol = builder.symbol
            val coneType = symbol.firSymbol.toConeType()
                .withNullability(nullable = builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
            return coneType.withAttributes(builder.annotations).asKaType() as KaTypeParameterType
        }

    override fun capturedType(
        type: KaCapturedType,
        init: KaCapturedTypeBuilder.() -> Unit,
    ): KaCapturedType {
        withValidityAssertion {
            val builder = KaBaseCapturedTypeBuilder.Base(this).apply(init)
            return type.coneType.withAttributes(builder.annotations)
                .withNullability(builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
                .asKaType() as KaCapturedType
        }
    }

    override fun capturedType(
        projection: KaTypeProjection,
        init: KaCapturedTypeBuilder.() -> Unit,
    ): KaCapturedType {
        withValidityAssertion {
            with(analysisSession) {
                if ((projection as? KaTypeArgumentWithVariance)?.variance == Variance.INVARIANT) {
                    errorWithAttachment("Only non-invariant projections can be captured") {
                        withEntry("projection", Variance.INVARIANT.toString())
                        withConeTypeEntry("type", projection.type.coneType)
                    }
                }

                val builder = KaBaseCapturedTypeBuilder.Base(this@KaFirTypeCreator).apply(init)
                return ConeCapturedType(
                    isMarkedNullable = builder.isMarkedNullable,
                    constructor = ConeCapturedTypeConstructor(
                        projection = projection.coneTypeProjection,
                        lowerType = null,
                        captureStatus = CaptureStatus.FROM_EXPRESSION,
                        supertypes = projection.type?.directSupertypes?.map { it.coneType }?.toList() ?: emptyList(),
                        typeParameterMarker = (projection.type as? KaTypeParameterType)?.symbol?.firSymbol?.toLookupTag()
                    ),
                    attributes = constructConeAttributes(builder.annotations)
                ).asKaType() as KaCapturedType
            }
        }
    }

    override fun definitelyNotNullType(
        type: KaType,
        init: KaDefinitelyNotNullTypeBuilder.() -> Unit,
    ): KaDefinitelyNotNullType {
        withValidityAssertion {
            with(analysisSession) {
                val builder = KaBaseDefinitelyNotNullTypeBuilder.Base(this@KaFirTypeCreator).apply(init)
                if (type !is KaCapturedType && type !is KaTypeParameterType) {
                    errorWithAttachment("`KaDefinitelyNotNullType` can only wrap `KaCapturedType` or `KaTypeParameterType`") {
                        withConeTypeEntry("type", type.coneType)
                    }
                }

                val coneType = type.coneType as ConeSimpleKotlinType
                val definitelyNotNullConeType =
                    ConeDefinitelyNotNullType.create(coneType, analysisSession.firSession.typeContext, avoidComprehensiveCheck = true)
                        ?: errorWithAttachment("Unable to create a definitely not null type") {
                            withConeTypeEntry("type", coneType)
                        }
                return definitelyNotNullConeType.withAttributes(builder.annotations).asKaType() as KaDefinitelyNotNullType
            }
        }
    }

    override fun flexibleType(
        type: KaFlexibleType,
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaFlexibleType {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder.ByFlexibleType(type, this).apply(init)
            return buildFlexibleType(builder)
        }
    }

    override fun flexibleType(
        lowerBound: KaType,
        upperBound: KaType,
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaFlexibleType {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder.ByBounds(lowerBound, upperBound, this).apply(init)
            return buildFlexibleType(builder)
        }
    }

    private fun buildFlexibleType(builder: KaFlexibleTypeBuilder): KaFlexibleType {
        withValidityAssertion {
            val lowerBound = builder.lowerBound.coneType.lowerBoundIfFlexible()
            val upperBound = builder.upperBound.coneType.upperBoundIfFlexible()

            if (AbstractStrictEqualityTypeChecker.strictEqualTypes(typeContext, lowerBound, upperBound)) {
                errorWithAttachment("Lower and upper bounds are equal") {
                    withConeTypeEntry("lowerBound", lowerBound)
                    withConeTypeEntry("upperBound", upperBound)
                }
            }

            if (!lowerBound.isSubtypeOf(upperBound, rootModuleSession)) {
                errorWithAttachment("Lower bound must be a subtype of upper bound") {
                    withConeTypeEntry("lowerBound", lowerBound)
                    withConeTypeEntry("upperBound", upperBound)
                }
            }

            val coneType = typeContext.createFlexibleType(lowerBound, upperBound) as ConeKotlinType
            return coneType.withAttributes(builder.annotations).asKaType() as KaFlexibleType
        }
    }

    override fun intersectionType(
        type: KaIntersectionType,
        init: KaIntersectionTypeBuilder.() -> Unit,
    ): KaIntersectionType {
        withValidityAssertion {
            val builder = KaBaseIntersectionTypeBuilder.ByIntersectionType(type, this).apply(init)
            return buildIntersectionType(builder)
        }
    }

    override fun intersectionType(
        conjuncts: List<KaType>,
        init: KaIntersectionTypeBuilder.() -> Unit,
    ): KaIntersectionType {
        withValidityAssertion {
            val builder = KaBaseIntersectionTypeBuilder.ByConjuncts(conjuncts, this).apply(init)
            return buildIntersectionType(builder)
        }
    }

    private fun buildIntersectionType(builder: KaIntersectionTypeBuilder): KaIntersectionType {
        val conjuncts = builder.conjuncts
        assert(conjuncts.isNotEmpty()) { "Intersection type must have at least one conjunct" }

        val coneType = ConeIntersectionType(conjuncts.map { it.coneType })
        return coneType.withAttributes(builder.annotations).asKaType() as KaIntersectionType
    }

    override fun dynamicType(init: KaDynamicTypeBuilder.() -> Unit): KaDynamicType {
        withValidityAssertion {
            val builder = KaBaseDynamicTypeBuilder(this).apply(init)
            val coneType = ConeDynamicType.create(rootModuleSession, attributes = constructConeAttributes(builder.annotations))
            return coneType.asKaType() as KaDynamicType
        }
    }

    override fun functionType(
        init: KaFunctionTypeBuilder.() -> Unit,
    ): KaFunctionType = withValidityAssertion {
        return buildFunctionType(KaBaseFunctionTypeBuilder.Base(this, analysisSession).apply(init)) as KaFunctionType
    }


    private fun buildFunctionType(builder: KaBaseFunctionTypeBuilder): KaType {
        val isSuspend = builder.isSuspend
        val isReflect = builder.isReflectType
        val numberOfParameters =
            builder.valueParameters.size +
                    (builder.contextParameters.size.takeIf { !isReflect } ?: 0) +
                    (builder.receiverType?.let { 1 } ?: 0)

        val baseClassId = when {
            isSuspend && isReflect -> StandardNames.getKSuspendFunctionClassId(numberOfParameters)
            isSuspend -> StandardNames.getSuspendFunctionClassId(numberOfParameters)
            isReflect -> StandardNames.getKFunctionClassId(numberOfParameters)
            else -> StandardNames.getFunctionClassId(numberOfParameters)
        }

        val firAnnotation = builder.annotations.mapNotNull { constructAnnotation(it) }

        val refinedClassId =
            analysisSession.firSession.functionTypeService.extractSingleExtensionKindForDeserializedConeType(baseClassId, firAnnotation)
                ?.let { functionClassKind ->
                    ClassId(functionClassKind.packageFqName, functionClassKind.numberedClassName(builder.valueParameters.size))
                } ?: baseClassId

        val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(refinedClassId)
            ?: return ConeErrorType(ConeUnresolvedSymbolError(refinedClassId)).asKaType()
        val lookupTag = classSymbol.toLookupTag()

        val contextParameters = builder.contextParameters.map { it.coneType }.butIf(isReflect) { emptyList() }
        val receiverType = builder.receiverType?.coneType
        val valueParameters = builder.valueParameters.map { valueParameter ->
            val parameterConeType = valueParameter.type.coneType
            valueParameter.name?.let { name ->
                parameterConeType.withParameterNameAnnotation(
                    name = name,
                    element = null
                )
            } ?: parameterConeType
        }

        val returnType = builder.returnType.coneType

        val typeArguments = buildList {
            addAll(contextParameters)
            addIfNotNull(receiverType)
            addAll(valueParameters)
            add(returnType)
        }

        val constructedAttributes = constructAttributes(builder.annotations).let { attributes ->
            if (contextParameters.isNotEmpty()) {
                attributes + CompilerConeAttributes.ContextFunctionTypeParams(contextParameters.size)
            } else {
                attributes
            }
        }

        val typeContext = rootModuleSession.typeContext
        val coneType = typeContext.createSimpleType(
            constructor = lookupTag,
            arguments = typeArguments,
            nullable = builder.isMarkedNullable,
            isExtensionFunction = builder.receiverType != null,
            attributes = constructedAttributes
        ) as ConeClassLikeType

        return coneType.asKaType()
    }

    private fun ConeKotlinType.withAttributes(annotationClassIds: List<ClassId>): ConeKotlinType {
        return this.withAttributes(constructConeAttributes(annotationClassIds))
    }

    private fun constructConeAttributes(annotationClassIds: List<ClassId>): ConeAttributes {
        return ConeAttributes.create(constructAttributes(annotationClassIds))
    }

    private fun constructAttributes(annotationClassIds: List<ClassId>): List<ConeAttribute<*>> {
        if (annotationClassIds.isEmpty()) {
            return emptyList()
        }

        val customAttribute = CustomAnnotationTypeAttribute(annotationClassIds.mapNotNull(::constructAnnotation))

        return listOf(customAttribute)
    }

    private fun constructAnnotation(classId: ClassId): FirAnnotation? {
        val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return null

        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                this.coneType = classSymbol.defaultType()
            }
            argumentMapping = FirEmptyAnnotationArgumentMapping
        }
    }
}