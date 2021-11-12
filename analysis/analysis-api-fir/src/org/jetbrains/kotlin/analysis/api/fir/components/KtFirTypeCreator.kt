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
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext

internal class KtFirTypeCreator(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken
) : KtTypeCreator(), KtFirAnalysisSessionComponent {

    override fun buildClassType(builder: KtClassTypeBuilder): KtClassType = withValidityAssertion {
        val lookupTag = when (builder) {
            is KtClassTypeBuilder.ByClassId -> {
                val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(builder.classId)
                    ?: return ConeClassErrorType(ConeUnresolvedSymbolError(builder.classId)).asKtType() as KtClassType
                classSymbol.toLookupTag()
            }
            is KtClassTypeBuilder.BySymbol -> {
                val symbol = builder.symbol
                check(symbol is KtFirSymbol<*>)
                symbol.firRef.withFir { (it as FirClassLikeDeclaration).symbol.toLookupTag() }
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

    override fun buildTypeParameterType(builder: KtTypeParameterTypeBuilder): KtTypeParameterType = withValidityAssertion {
        val coneType = when (builder) {
            is KtTypeParameterTypeBuilder.BySymbol -> {
                val symbol = builder.symbol
                check(symbol is KtFirSymbol<*>)
                symbol.firRef.withFir { (it as FirTypeParameter).toConeType() }
            }
        }
        return coneType.asKtType() as KtTypeParameterType
    }
}
