/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.typeCreation

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.asKaType
import org.jetbrains.kotlin.analysis.api.fir.utils.coneTypeProjection
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.*
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirTypeCreator(
    analysisSession: KaFirSession,
) : KaBaseTypeCreator<KaFirSession>(analysisSession) {
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
                    ?: return ConeErrorType(ConeUnresolvedSymbolError(builder.classId)).asKaType()
                classSymbol.toLookupTag()
            }
            is KaBaseClassTypeBuilder.BySymbol -> {
                val symbol = builder.symbol
                symbol.classId?.toLookupTag() ?: symbol.firSymbol.toLookupTag()
            }
        }

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

        return coneType.asKaType()
    }

    override fun typeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType =
        withValidityAssertion {
            val builder = KaBaseTypeParameterTypeBuilder(this).apply(init)
            val coneType = symbol.firSymbol.toConeType()
                .withNullability(nullable = builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
            return coneType.asKaType() as KaTypeParameterType
        }

    override fun capturedType(
        type: KaCapturedType,
        init: KaCapturedTypeBuilder.() -> Unit,
    ): KaCapturedType {
        withValidityAssertion {
            val builder = KaBaseCapturedTypeBuilder.Base(this).apply(init)
            return type.coneType.withNullability(builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
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
                    )
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
                return definitelyNotNullConeType.asKaType() as KaDefinitelyNotNullType
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
            return coneType.asKaType() as KaFlexibleType
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
        return coneType.asKaType() as KaIntersectionType
    }

    override fun dynamicType(): KaDynamicType {
        withValidityAssertion {
            val coneType = ConeDynamicType.create(rootModuleSession)
            return coneType.asKaType() as KaDynamicType
        }
    }


    private fun ConeKotlinType.asKaType(): KaType = asKaType(analysisSession)

    private val KaTypeProjection.coneTypeProjection: ConeTypeProjection
        get() = coneTypeProjection(analysisSession)

    private val rootModuleSession: FirSession get() = analysisSession.resolutionFacade.useSiteFirSession
    private val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
}