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
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.ClassId
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
            val builder = KaBaseCapturedTypeBuilder(this).apply(init)
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

                val builder = KaBaseCapturedTypeBuilder(this@KaFirTypeCreator).apply(init)
                val capturedType = firSession.typeContext.createCapturedType(
                    projection.coneTypeProjection,
                    projection.type?.directSupertypes?.map { it.coneType }?.toList() ?: emptyList(),
                    lowerType = null,
                    CaptureStatus.FROM_EXPRESSION
                ) as ConeCapturedType

                return capturedType.withNullability(builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
                    .asKaType() as KaCapturedType
            }
        }
    }

    override fun definitelyNotNullType(
        type: KaTypeParameterType
    ): KaType = withValidityAssertion { buildDefinitelyNotNullType(type) }

    override fun definitelyNotNullType(
        type: KaCapturedType
    ): KaType = withValidityAssertion { buildDefinitelyNotNullType(type) }

    private fun buildDefinitelyNotNullType(type: KaType): KaType {
        val coneType = type.coneType as ConeSimpleKotlinType
        val definitelyNotNullConeType =
            ConeDefinitelyNotNullType.create(coneType, analysisSession.firSession.typeContext)
                ?: coneType
        return definitelyNotNullConeType.asKaType()
    }

    override fun flexibleType(
        type: KaFlexibleType,
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaType? {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder.ByFlexibleType(type, this).apply(init)
            return buildFlexibleType(builder)
        }
    }

    override fun flexibleType(
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaType? {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder.WithDefaults(analysisSession, this).apply(init)
            return buildFlexibleType(builder)
        }
    }

    private fun buildFlexibleType(builder: KaFlexibleTypeBuilder): KaType? {
        withValidityAssertion {
            with(analysisSession) {
                val lowerBound = builder.lowerBound.lowerBoundIfFlexible()
                val upperBound = builder.upperBound.upperBoundIfFlexible()

                if (lowerBound == upperBound) {
                    return lowerBound
                }

                if (!lowerBound.isSubtypeOf(upperBound)) {
                    return null
                }

                val coneLowerBound = lowerBound.coneType as ConeRigidType
                val coneUpperBound = upperBound.coneType as ConeRigidType

                val coneType = typeContext.createFlexibleType(coneLowerBound, coneUpperBound) as ConeKotlinType
                return coneType.asKaType() as KaFlexibleType
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
                    return builtinTypes.nullableAny
                }

                val coneType = ConeTypeIntersector.intersectTypes(
                    firSession.typeContext,
                    conjuncts.map { it.coneType }
                )
                return coneType.asKaType()
            }
        }
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