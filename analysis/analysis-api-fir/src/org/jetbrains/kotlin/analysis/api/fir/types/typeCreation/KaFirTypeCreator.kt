/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.typeCreation

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirStarTypeProjection
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.UsualClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.asKaType
import org.jetbrains.kotlin.analysis.api.fir.utils.coneType
import org.jetbrains.kotlin.analysis.api.fir.utils.coneTypeProjection
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.isAnnotationClass
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.computeTypeAttributes
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.getPrimaryConstructorSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.withParameterNameAnnotation
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.butIf

internal class KaFirTypeCreator(
    val analysisSession: KaFirSession,
) : KaTypeCreator {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun typeProjection(
        variance: Variance,
        type: KaType,
    ): KaTypeArgumentWithVariance = withValidityAssertion {
        KaFirTypeArgumentWithVariance(type, variance, token)
    }

    override fun typeProjection(
        variance: Variance,
        type: KaTypeCreator.() -> KaType,
    ): KaTypeArgumentWithVariance = withValidityAssertion {
        KaFirTypeArgumentWithVariance(type(), variance, token)
    }

    override fun starTypeProjection(): KaStarTypeProjection = withValidityAssertion {
        KaFirStarTypeProjection(token)
    }

    override fun arrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        with(analysisSession) {
            val builder = KaFirArrayTypeBuilder(this@KaFirTypeCreator).apply(init)

            if (elementType is KaClassType && builder.shouldPreferPrimitiveTypes && !elementType.isMarkedNullable) {
                val classId = elementType.classId
                val primitiveArrayId =
                    StandardClassIds.primitiveArrayTypeByElementType[classId]
                        ?: StandardClassIds.unsignedArrayTypeByElementType[classId]
                if (primitiveArrayId != null) {
                    return classType(primitiveArrayId) {
                        isMarkedNullable = builder.isMarkedNullable
                        annotations(builder.annotations)
                    }
                }
            }

            return classType(StandardClassIds.Array) {
                isMarkedNullable = builder.isMarkedNullable
                typeArgument(builder.variance, elementType)
                annotations(builder.annotations)
            }
        }
    }

    override fun varargArrayType(elementType: KaType): KaType = withValidityAssertion {
        arrayType(elementType) {
            variance = Variance.OUT_VARIANCE
        }
    }

    override fun classType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(classId)
        if (classSymbol == null) {
            val builder = KaFirClassTypeBuilder(this, outerClassSymbolsProvider = ::emptyList).apply(init)
            return ConeErrorType(
                diagnostic = ConeUnresolvedSymbolError(classId),
                attributes = constructAnnotationAttributes(builder.annotations, allowExtensionFunctionType = false),
                nullable = builder.isMarkedNullable
            ).asKaType()
        }

        return buildClassType(classSymbol.toLookupTag(), classSymbol, capturedTypeArguments = emptyList(), init)
    }

    override fun classType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        classType(symbol, capturedTypeArguments = emptyList(), init)
    }

    /**
     * Same as [classType], but uses [capturedTypeArguments] as the arguments of the type parameters captured by a local class
     * from its containing declarations, which cannot be provided via [KaClassTypeBuilder].
     * Missing arguments default to the type parameters themselves.
     */
    internal fun classType(
        symbol: KaClassLikeSymbol,
        capturedTypeArguments: List<ConeTypeProjection>,
        init: KaClassTypeBuilder.() -> Unit,
    ): KaType = withValidityAssertion {
        val lookupTag = symbol.classId?.toLookupTag() ?: symbol.firSymbol.toLookupTag()
        return buildClassType(lookupTag, symbol.firSymbol, capturedTypeArguments, init)
    }

    private fun buildClassType(
        lookupTag: ConeClassLikeLookupTag,
        classSymbol: FirClassLikeSymbol<*>,
        capturedTypeArguments: List<ConeTypeProjection>,
        init: KaClassTypeBuilder.() -> Unit,
    ): KaType {
        val classDeclaration = classSymbol.fir
        val designation = UsualClassTypeQualifierBuilder.collectDesignationPath(classDeclaration)

        val builder = KaFirClassTypeBuilder(this) {
            designation.dropLast(1).map { outerClass ->
                analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(outerClass.symbol)
            }
        }.apply(init)

        val expectedNumberOfArgumentsWithSynthetics = with(typeContext) { lookupTag.parametersCount() }
        val expectedNumberOfArgumentsByQualifier = UsualClassTypeQualifierBuilder.countOwnTypeParametersByDesignation(designation)
        val expectedNumberOfOwnArguments = expectedNumberOfArgumentsByQualifier.last()
        val expectedNumbersOfOuterArguments = expectedNumberOfArgumentsByQualifier.dropLast(1)
        // In some rare invalid cases, the diff might be negative. To avoid further exceptions, coercion is needed.
        val expectedNumberOfSyntheticArguments =
            (expectedNumberOfArgumentsWithSynthetics - expectedNumberOfArgumentsByQualifier.sum()).coerceAtLeast(0)

        val arguments = buildList {
            fun addTypeArguments(typeArguments: List<KaTypeProjection>?, count: Int) {
                repeat(count) { index -> add(typeArguments?.getOrNull(index)?.coneTypeProjection ?: ConeStarProjection) }
            }

            addTypeArguments(builder.typeArguments, expectedNumberOfOwnArguments)

            val builderOuterArgumentsIfInitialized = builder.outerTypeArgumentsIfInitialized

            if (builderOuterArgumentsIfInitialized == null) {
                addTypeArguments(null, expectedNumbersOfOuterArguments.sum())
            } else {
                for (index in expectedNumbersOfOuterArguments.indices.reversed()) {
                    addTypeArguments(builderOuterArgumentsIfInitialized.getOrNull(index), expectedNumberOfArgumentsByQualifier[index])
                }
            }

            val defaultTypeSyntheticArguments = classSymbol.defaultType().typeArguments.takeLast(expectedNumberOfSyntheticArguments)
            repeat(expectedNumberOfSyntheticArguments) { index ->
                val syntheticArgument = capturedTypeArguments.getOrNull(index) ?: defaultTypeSyntheticArguments[index]
                add(syntheticArgument)
            }
        }

        val coneType = typeContext.createSimpleType(
            lookupTag,
            arguments,
            builder.isMarkedNullable
        ) as ConeClassLikeType

        return coneType.withAnnotationAttributes(builder.annotations).asKaType()
    }

    override fun typeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType =
        withValidityAssertion {
            val builder = KaFirTypeParameterTypeBuilder(this).apply(init)
            val coneType = symbol.firSymbol.toConeType()
                .withNullability(nullable = builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
            return coneType.withAnnotationAttributes(builder.annotations).asKaType() as KaTypeParameterType
        }

    override fun capturedType(
        type: KaCapturedType,
        init: KaCapturedTypeBuilder.() -> Unit,
    ): KaCapturedType {
        withValidityAssertion {
            val builder = KaFirCapturedTypeBuilder(this).apply(init)
            return type.coneType.withAnnotationAttributes(builder.annotations)
                .withNullability(builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
                .asKaType() as KaCapturedType
        }
    }

    override fun capturedType(
        projection: KaTypeProjection,
        init: KaCapturedTypeBuilder.() -> Unit,
    ): KaCapturedType? {
        withValidityAssertion {
            with(analysisSession) {
                if ((projection as? KaTypeArgumentWithVariance)?.variance == Variance.INVARIANT) {
                    return null
                }

                val builder = KaFirCapturedTypeBuilder(this@KaFirTypeCreator).apply(init)
                val capturedType = firSession.typeContext.createCapturedType(
                    projection.coneTypeProjection,
                    projection.type?.directSupertypes?.map { it.coneType }?.toList() ?: emptyList(),
                    lowerType = null,
                    CaptureStatus.FROM_EXPRESSION
                )

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
        buildDefinitelyNotNullType(type, KaFirDefinitelyNotNullTypeBuilder(this).apply(init))
    }

    override fun definitelyNotNullType(
        type: KaCapturedType,
        init: KaDefinitelyNotNullTypeBuilder.() -> Unit,
    ): KaType = withValidityAssertion {
        buildDefinitelyNotNullType(type, KaFirDefinitelyNotNullTypeBuilder(this).apply(init))
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
            val builder = KaFirFlexibleTypeBuilder(
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
                val builder = KaFirFlexibleTypeBuilder(
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
            val builder = KaFirFlexibleTypeBuilder(
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

                val coneType = typeContext.createFlexibleType(lowerBound, upperBound)
                return coneType.withAnnotationAttributes(builder.annotations).asKaType() as KaFlexibleType
            }
        }
    }

    override fun intersectionType(
        init: KaIntersectionTypeBuilder.() -> Unit,
    ): KaType {
        withValidityAssertion {
            with(analysisSession) {
                val builder = KaFirIntersectionTypeBuilder(this@KaFirTypeCreator).apply(init)
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
            val builder = KaFirDynamicTypeBuilder(this).apply(init)
            val attributes = constructAnnotationAttributes(builder.annotations, allowExtensionFunctionType = false)
            val coneType = ConeDynamicType.create(rootModuleSession, attributes = attributes)
            return coneType.asKaType() as KaDynamicType
        }
    }

    override fun functionType(
        init: KaFunctionTypeBuilder.() -> Unit,
    ): KaFunctionType = withValidityAssertion {
        return buildFunctionType(KaFirFunctionTypeBuilder(this, analysisSession).apply(init)) as KaFunctionType
    }


    private fun buildFunctionType(builder: KaFirFunctionTypeBuilder): KaType {
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

        val firAnnotations = builder.annotations.mapNotNull(::constructAnnotation)

        val refinedClassId =
            analysisSession.firSession.functionTypeService.extractSingleExtensionKindForDeserializedConeType(baseClassId, firAnnotations)
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

        val typeContext = rootModuleSession.typeContext
        val coneType = typeContext.createSimpleType(
            constructor = lookupTag,
            arguments = typeArguments,
            nullable = builder.isMarkedNullable,
            isExtensionFunction = builder.receiverType != null,
            contextParameterCount = contextParameters.size,
            attributes = firAnnotations.computeTypeAttributes(rootModuleSession, shouldExpandTypeAliases = false).toList()
        ) as ConeClassLikeType

        return coneType.asKaType()
    }

    private fun ConeKotlinType.withAnnotationAttributes(annotationClassIds: List<ClassId>): ConeKotlinType {
        // Same as in the compiler: `@ExtensionFunctionType` is only meaningful for function types
        val allowExtensionFunctionType = isSomeFunctionType(rootModuleSession)
        return this.withAttributes(constructAnnotationAttributes(annotationClassIds, allowExtensionFunctionType))
    }

    private fun constructAnnotationAttributes(annotationClassIds: List<ClassId>, allowExtensionFunctionType: Boolean): ConeAttributes {
        val annotations = annotationClassIds.mapNotNull(::constructAnnotation)
        return annotations.computeTypeAttributes(
            rootModuleSession,
            allowExtensionFunctionType = allowExtensionFunctionType,
            shouldExpandTypeAliases = false,
        )
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
