/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.typeCreation

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.asKaType
import org.jetbrains.kotlin.analysis.api.fir.utils.coneType
import org.jetbrains.kotlin.analysis.api.fir.utils.coneTypeProjection
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.*
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.isAnnotationClass
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.getPrimaryConstructorSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.withParameterNameAnnotation
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirTypeCreator(
    analysisSession: KaFirSession,
) : KaBaseTypeCreator<KaFirSession>(analysisSession) {
    override fun classType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        val builder = KaBaseClassTypeBuilder(this).apply(init)
        val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return ConeErrorType(
                ConeUnresolvedSymbolError(classId)
            ).withAnnotationAttributes(builder.annotations).asKaType()
        val lookupTag = classSymbol.toLookupTag()

        return buildClassType(lookupTag, builder)
    }

    override fun classType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        val symbol = symbol
        val lookupTag = symbol.classId?.toLookupTag() ?: symbol.firSymbol.toLookupTag()
        return buildClassType(lookupTag, KaBaseClassTypeBuilder(this).apply(init))
    }

    private fun buildClassType(lookupTag: ConeClassLikeLookupTag, builder: KaBaseClassTypeBuilder): KaClassType {
        val expectedNumberOfParameters = with(analysisSession.firSession.typeContext) { lookupTag.parametersCount() }
        val builderTypeArguments = builder.typeArguments
        val arguments = List(expectedNumberOfParameters) { index ->
            when (val builderArgument = builderTypeArguments.getOrNull(index)) {
                null -> ConeStarProjection
                else -> builderArgument.coneTypeProjection
            }
        }

        val typeContext = rootModuleSession.typeContext
        val coneType = typeContext.createSimpleType(
            lookupTag,
            arguments,
            builder.isMarkedNullable
        ) as ConeClassLikeType

        return coneType.withAnnotationAttributes(builder.annotations)
            .asKaType() as KaClassType
    }

    override fun typeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType =
        withValidityAssertion {
            val builder = KaBaseTypeParameterTypeBuilder(this).apply(init)
            val coneType = symbol.firSymbol.toConeType()
                .withNullability(nullable = builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
            return coneType.withAnnotationAttributes(builder.annotations).asKaType() as KaTypeParameterType
        }

    override fun capturedType(
        type: KaCapturedType,
        init: KaCapturedTypeBuilder.() -> Unit,
    ): KaCapturedType {
        withValidityAssertion {
            val builder = KaBaseCapturedTypeBuilder(this).apply(init)
            return type.coneType.withAnnotationAttributes(builder.annotations)
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

                val builder = KaBaseCapturedTypeBuilder(this@KaFirTypeCreator).apply(init)
                val capturedType = firSession.typeContext.createCapturedType(
                    projection.coneTypeProjection,
                    projection.type?.directSupertypes?.map { it.coneType }?.toList() ?: emptyList(),
                    lowerType = null,
                    CaptureStatus.FROM_EXPRESSION
                ) as ConeCapturedType

                return capturedType
                    .withNullability(builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
                    .withAnnotationAttributes(builder.annotations)
                    .asKaType() as KaCapturedType
            }
        }
    }

    override fun definitelyNotNullType(
        type: KaTypeParameterType,
        init: KaDefinitelyNotNullTypeBuilder.() -> Unit,
    ): KaType = withValidityAssertion {
        buildDefinitelyNotNullType(type, KaBaseDefinitelyNotNullTypeBuilder(this).apply(init))
    }

    override fun definitelyNotNullType(
        type: KaCapturedType,
        init: KaDefinitelyNotNullTypeBuilder.() -> Unit,
    ): KaType = withValidityAssertion {
        buildDefinitelyNotNullType(type, KaBaseDefinitelyNotNullTypeBuilder(this).apply(init))
    }

    private fun buildDefinitelyNotNullType(type: KaType, builder: KaDefinitelyNotNullTypeBuilder): KaType {
        val coneType = type.coneType as ConeSimpleKotlinType
        val definitelyNotNullConeType =
            ConeDefinitelyNotNullType.create(coneType, analysisSession.firSession.typeContext)
                ?: coneType
        return definitelyNotNullConeType.withAnnotationAttributes(builder.annotations).asKaType()
    }

    override fun flexibleType(
        type: KaFlexibleType,
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaType? {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder(
                type.lowerBound,
                type.upperBound,
                this
            ).apply(init)
            return buildFlexibleType(builder)
        }
    }

    override fun flexibleType(
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaType? {
        withValidityAssertion {
            with(analysisSession) {
                val builder = KaBaseFlexibleTypeBuilder(
                    lowerBound = builtinTypes.nothing,
                    upperBound = builtinTypes.nullableAny,
                    this@KaFirTypeCreator
                ).apply(init)
                return buildFlexibleType(builder)
            }
        }
    }

    override fun flexibleType(
        lowerBound: KaType,
        upperBound: KaType,
    ): KaType? {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder(
                lowerBound,
                upperBound,
                this
            )
            return buildFlexibleType(builder)
        }
    }

    private fun buildFlexibleType(builder: KaFlexibleTypeBuilder): KaType? {
        withValidityAssertion {
            with(analysisSession) {
                val lowerBound = builder.lowerBound.coneType.lowerBoundIfFlexible()
                val upperBound = builder.upperBound.coneType.upperBoundIfFlexible()

                if (lowerBound == upperBound) {
                    return lowerBound.withAnnotationAttributes(builder.annotations).asKaType()
                }

                if (!lowerBound.isSubtypeOf(upperBound, firSession)) {
                    return null
                }

                val coneType = typeContext.createFlexibleType(lowerBound, upperBound) as ConeKotlinType
                return coneType.withAnnotationAttributes(builder.annotations).asKaType() as KaFlexibleType
            }
        }
    }

    override fun intersectionType(
        init: KaIntersectionTypeBuilder.() -> Unit,
    ): KaType {
        withValidityAssertion {
            with(analysisSession) {
                val builder = KaBaseIntersectionTypeBuilder(this@KaFirTypeCreator).apply(init)
                val conjuncts = builder.conjuncts

                if (conjuncts.isEmpty()) {
                    return builtinTypes.nullableAny.coneType.asKaType()
                }

                val coneType = ConeTypeIntersector.intersectTypes(
                    firSession.typeContext,
                    conjuncts.map { it.coneType }
                )
                return coneType.asKaType()
            }
        }
    }

    override fun dynamicType(init: KaDynamicTypeBuilder.() -> Unit): KaDynamicType {
        withValidityAssertion {
            val builder = KaBaseDynamicTypeBuilder(this).apply(init)
            val coneType = ConeDynamicType.create(rootModuleSession, attributes = constructAnnotationAttributes(builder.annotations))
            return coneType.asKaType() as KaDynamicType
        }
    }

    override fun functionType(
        init: KaFunctionTypeBuilder.() -> Unit,
    ): KaFunctionType = withValidityAssertion {
        return buildFunctionType(KaBaseFunctionTypeBuilder(this, analysisSession).apply(init)) as KaFunctionType
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
                    ClassId(functionClassKind.packageFqName, functionClassKind.numberedClassName(numberOfParameters))
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

        val constructedAttributes = constructAnnotationAttributesList(builder.annotations)
            .let { attributes ->
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

    private fun ConeKotlinType.withAnnotationAttributes(annotationClassIds: List<ClassId>): ConeKotlinType {
        return this.withAttributes(constructAnnotationAttributes(annotationClassIds))
    }

    private fun constructAnnotationAttributes(annotationClassIds: List<ClassId>): ConeAttributes {
        return ConeAttributes.create(constructAnnotationAttributesList(annotationClassIds))
    }

    private fun constructAnnotationAttributesList(annotationClassIds: List<ClassId>): List<ConeAttribute<*>> {
        if (annotationClassIds.isEmpty()) {
            return emptyList()
        }

        val customAttribute = CustomAnnotationTypeAttribute(annotationClassIds.mapNotNull(::constructAnnotation))

        return listOf(customAttribute)
    }

    private fun constructAnnotation(classId: ClassId): FirAnnotation? {
        val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return null

        if (classSymbol.classKind?.isAnnotationClass != true) {
            return null
        }

        val firSession = analysisSession.firSession
        val primaryConstructor = classSymbol.getPrimaryConstructorSymbol(firSession, firSession.getScopeSession()) ?: return null

        if (primaryConstructor.valueParameterSymbols.isNotEmpty()) {
            return null
        }

        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                this.coneType = classSymbol.defaultType()
            }
            argumentMapping = FirEmptyAnnotationArgumentMapping
        }
    }


    private fun ConeKotlinType.asKaType(): KaType = asKaType(analysisSession)

    private val KaTypeProjection.coneTypeProjection: ConeTypeProjection
        get() = coneTypeProjection(analysisSession)

    private val rootModuleSession: FirSession get() = analysisSession.resolutionFacade.useSiteFirSession
    private val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
}