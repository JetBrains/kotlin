/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.qualifiers

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirUnresolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.toConeTypeProjection

internal object ErrorClassTypeQualifierBuilder {
    fun createQualifiersForUnresolvedType(
        coneDiagnostic: ConeUnresolvedError,
        builder: KaSymbolByFirBuilder,
    ): List<KaClassTypeQualifier> {
        return when (coneDiagnostic) {
            is ConeUnresolvedTypeQualifierError ->
                coneDiagnostic.qualifiers.map { part ->
                    KaFirUnresolvedClassTypeQualifier(
                        part.name,
                        part.typeArgumentList.typeArguments.map { builder.typeBuilder.buildTypeProjection(it.toConeTypeProjection()) },
                        builder.token
                    )
                }

            is ConeUnresolvedNameError -> listOf(
                KaFirUnresolvedClassTypeQualifier(coneDiagnostic.name, emptyList(), builder.token)
            )

            is ConeUnresolvedReferenceError -> listOf(
                KaFirUnresolvedClassTypeQualifier(coneDiagnostic.name, emptyList(), builder.token)
            )

            is ConeUnresolvedSymbolError ->
                coneDiagnostic.classId.asSingleFqName().pathSegments()
                    .map { KaFirUnresolvedClassTypeQualifier(it, emptyList(), builder.token) }

        }
    }

    fun createQualifiersForUnmatchedTypeArgumentsType(
        coneDiagnostic: ConeUnmatchedTypeArgumentsError,
        builder: KaSymbolByFirBuilder
    ): List<KaClassTypeQualifier> {
        return createQualifiersByClassSymbol(coneDiagnostic.symbol, builder)
    }

    /**
     * Returns the qualifiers of [firSymbol] and its containing classes, ordered outermost-first.
     */
    private fun createQualifiersByClassSymbol(
        firSymbol: FirClassLikeSymbol<*>,
        builder: KaSymbolByFirBuilder
    ): List<KaResolvedClassTypeQualifier> {
        return UsualClassTypeQualifierBuilder.collectDesignation(firSymbol).map { qualifierSymbol ->
            KaFirResolvedClassTypeQualifier(
                builder.classifierBuilder.buildClassLikeSymbol(qualifierSymbol.symbol),
                emptyList(),
            )
        }
    }
}
