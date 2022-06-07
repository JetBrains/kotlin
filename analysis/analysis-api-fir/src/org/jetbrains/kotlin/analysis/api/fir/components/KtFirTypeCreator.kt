/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KtTypeCreator
import org.jetbrains.kotlin.analysis.api.components.KtTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext

internal class KtFirTypeCreator(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken
) : KtTypeCreator(), KtFirAnalysisSessionComponent {

    override fun buildClassType(builder: KtClassTypeBuilder): KtClassType {
        val lookupTag = when (builder) {
            is KtClassTypeBuilder.ByClassId -> {
                val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(builder.classId)
                    ?: return ConeErrorType(ConeUnresolvedSymbolError(builder.classId)).asKtType() as KtClassType
                classSymbol.toLookupTag()
            }
            is KtClassTypeBuilder.BySymbol -> {
                val symbol = builder.symbol
                check(symbol is KtFirSymbol<*>)
                (symbol.firSymbol as FirClassLikeSymbol<*>).toLookupTag()
            }
        }

        val typeContext = rootModuleSession.typeContext
        val coneType = typeContext.createSimpleType(
            lookupTag,
            builder.arguments.map { it.coneTypeProjection },
            builder.nullability.isNullable
        ) as ConeClassLikeType

        return coneType.asKtType() as KtClassType
    }

    override fun buildTypeParameterType(builder: KtTypeParameterTypeBuilder): KtTypeParameterType  {
        val coneType = when (builder) {
            is KtTypeParameterTypeBuilder.BySymbol -> {
                val symbol = builder.symbol
                (symbol as KtFirTypeParameterSymbol).firSymbol.toConeType()
            }
        }
        return coneType.asKtType() as KtTypeParameterType
    }
}
