/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.qualifiers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirUnresolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.toConeTypeProjection

internal object ErrorClassTypeQualifierBuilder {
    context(analysisSession: KaFirSession)
    fun createQualifiersForUnresolvedType(
        coneDiagnostic: ConeUnresolvedError,
    ): List<KaClassTypeQualifier> {
        return when (coneDiagnostic) {
            is ConeUnresolvedTypeQualifierError ->
                coneDiagnostic.qualifiers.map { part ->
                    KaFirUnresolvedClassTypeQualifier(
                        part.name,
                        part.typeArgumentList.typeArguments.map {
                            firSymbolBuilder.typeBuilder.buildTypeProjection(it.toConeTypeProjection())
                        },
                        analysisSession.token
                    )
                }

            is ConeUnresolvedNameError -> listOf(
                KaFirUnresolvedClassTypeQualifier(coneDiagnostic.name, emptyList(), analysisSession.token)
            )

            is ConeUnresolvedReferenceError -> listOf(
                KaFirUnresolvedClassTypeQualifier(coneDiagnostic.name, emptyList(), analysisSession.token)
            )

            is ConeUnresolvedSymbolError ->
                coneDiagnostic.classId.asSingleFqName().pathSegments()
                    .map { KaFirUnresolvedClassTypeQualifier(it, emptyList(), analysisSession.token) }

        }
    }

    context(analysisSession: KaFirSession)
    fun createQualifiersForUnmatchedTypeArgumentsType(
        coneDiagnostic: ConeUnmatchedTypeArgumentsError
    ): List<KaClassTypeQualifier> {
        return createQualifiersByClassSymbol(coneDiagnostic.symbol)
    }

    context(analysisSession: KaFirSession)
    private fun createQualifiersByClassSymbol(
        firSymbol: FirClassLikeSymbol<*>
    ): List<KaResolvedClassTypeQualifier> {
        return generateSequence(firSymbol) { it.getContainingClassSymbol() }.mapTo(mutableListOf()) { classSymbol ->
            KaFirResolvedClassTypeQualifier(
                firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(classSymbol),
                emptyList(),
            )
        }
    }
}
