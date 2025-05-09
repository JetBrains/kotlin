/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseTypeCreator
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId

internal class KaFirTypeCreator(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseTypeCreator<KaFirSession>(), KaFirSessionComponent {
    override fun buildClassType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        return buildClassType(KaBaseClassTypeBuilder.ByClassId(classId, token).apply(init))
    }

    override fun buildClassType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        return buildClassType(KaBaseClassTypeBuilder.BySymbol(symbol, token).apply(init))
    }

    private fun buildClassType(builder: KaBaseClassTypeBuilder): KaType {
        val lookupTag = when (builder) {
            is KaBaseClassTypeBuilder.ByClassId -> {
                val classSymbol = rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(builder.classId)
                    ?: return ConeErrorType(ConeUnresolvedSymbolError(builder.classId)).asKtType()
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
            builder.arguments.map { it.coneTypeProjection },
            builder.isMarkedNullable
        ) as ConeClassLikeType

        return coneType.asKtType()
    }

    override fun buildTypeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType {
        withValidityAssertion {
            val builder = KaBaseTypeParameterTypeBuilder.BySymbol(symbol, token).apply(init)
            val symbol = builder.symbol
            val coneType = symbol.firSymbol.toConeType()
                .withNullability(nullable = builder.isMarkedNullable, typeContext = analysisSession.firSession.typeContext)
            return coneType.asKtType() as KaTypeParameterType
        }
    }
}
