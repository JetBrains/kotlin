/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.idea.frontend.api.components.KtClassTypeBuilder
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeCreator
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion

internal class KtFirTypeCreator(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken
) : KtTypeCreator(), KtFirAnalysisSessionComponent {

    override fun buildClassType(builder: KtClassTypeBuilder): KtClassType = withValidityAssertion {
        val lookupTag = when (builder) {
            is KtClassTypeBuilder.ByClassId -> {
                val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByFqName(builder.classId)
                    ?: return ConeClassErrorType(ConeUnresolvedSymbolError(builder.classId)).asKtType() as KtClassType
                classSymbol.toLookupTag()
            }
            is KtClassTypeBuilder.BySymbol -> {
                val symbol = builder.symbol
                check(symbol is KtFirSymbol<*>)
                symbol.firRef.withFir { (it as FirClassLikeDeclaration<*>).symbol.toLookupTag() }
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
}