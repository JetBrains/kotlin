/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.typeCreation

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.asKaType
import org.jetbrains.kotlin.analysis.api.fir.utils.coneTypeProjection
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.KaBaseClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.KaBaseTypeCreator
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.KaBaseTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId

internal class KaFirTypeCreator(
    analysisSession: KaFirSession,
) : KaBaseTypeCreator<KaFirSession>(analysisSession) {
    override fun classType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        val builder = KaBaseClassTypeBuilder(this).apply(init)
        val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return ConeErrorType(
                ConeUnresolvedSymbolError(classId)
            ).asKaType()
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

        return coneType.asKaType() as KaClassType
    }

    override fun typeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType =
        withValidityAssertion {
            val builder = KaBaseTypeParameterTypeBuilder(this).apply(init)
            val coneType = symbol.firSymbol.toConeType()
                .withNullability(nullable = builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
            return coneType.asKaType() as KaTypeParameterType
        }

    private fun ConeKotlinType.asKaType(): KaType = asKaType(analysisSession)

    private val KaTypeProjection.coneTypeProjection: ConeTypeProjection
        get() = coneTypeProjection(analysisSession)

    private val rootModuleSession: FirSession get() = analysisSession.resolutionFacade.useSiteFirSession
    private val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
}