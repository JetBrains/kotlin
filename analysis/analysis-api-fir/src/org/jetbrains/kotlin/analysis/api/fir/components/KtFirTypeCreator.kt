/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KaTypeCreator
import org.jetbrains.kotlin.analysis.api.components.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext

internal class KaFirTypeCreator(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken
) : KaTypeCreator(), KaFirSessionComponent {

    override fun buildClassType(builder: KaClassTypeBuilder): KaType {
        val lookupTag = when (builder) {
            is KaClassTypeBuilder.ByClassId -> {
                val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(builder.classId)
                    ?: return ConeErrorType(ConeUnresolvedSymbolError(builder.classId)).asKtType()
                classSymbol.toLookupTag()
            }
            is KaClassTypeBuilder.BySymbol -> {
                val symbol = builder.symbol
                check(symbol is KaFirSymbol<*>)
                (symbol.firSymbol as FirClassLikeSymbol<*>).toLookupTag()
            }
        }

        val typeContext = rootModuleSession.typeContext
        val coneType = typeContext.createSimpleType(
            lookupTag,
            builder.arguments.map { it.coneTypeProjection },
            builder.nullability.isNullable
        ) as ConeClassLikeType

        return coneType.asKtType()
    }

    override fun buildTypeParameterType(builder: KaTypeParameterTypeBuilder): KaTypeParameterType {
        val coneType = when (builder) {
            is KaTypeParameterTypeBuilder.BySymbol -> {
                val symbol = builder.symbol
                (symbol as KaFirTypeParameterSymbol).firSymbol.toConeType()
            }
        }
        return coneType.asKtType() as KaTypeParameterType
    }
}
