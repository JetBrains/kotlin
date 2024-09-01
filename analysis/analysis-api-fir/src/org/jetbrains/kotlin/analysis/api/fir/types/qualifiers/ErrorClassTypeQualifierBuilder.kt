/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.qualifiers

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseUnresolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
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
                    KaBaseUnresolvedClassTypeQualifier(
                        part.name,
                        part.typeArgumentList.typeArguments.map { builder.typeBuilder.buildTypeProjection(it.toConeTypeProjection()) },
                        builder.token
                    )
                }

            is ConeUnresolvedNameError -> listOf(
                KaBaseUnresolvedClassTypeQualifier(coneDiagnostic.name, emptyList(), builder.token)
            )

            is ConeUnresolvedReferenceError -> listOf(
                KaBaseUnresolvedClassTypeQualifier(coneDiagnostic.name, emptyList(), builder.token)
            )

            is ConeUnresolvedSymbolError ->
                coneDiagnostic.classId.asSingleFqName().pathSegments()
                    .map { KaBaseUnresolvedClassTypeQualifier(it, emptyList(), builder.token) }

        }
    }

    fun createQualifiersForUnmatchedTypeArgumentsType(
        coneDiagnostic: ConeUnmatchedTypeArgumentsError,
        builder: KaSymbolByFirBuilder
    ): List<KaClassTypeQualifier> {
        return createQualifiersByClassSymbol(coneDiagnostic.symbol, builder)
    }

    private fun createQualifiersByClassSymbol(
        firSymbol: FirClassLikeSymbol<*>,
        builder: KaSymbolByFirBuilder
    ): List<KaResolvedClassTypeQualifier> {
        return generateSequence(firSymbol) { it.getContainingClassSymbol() }.mapTo(mutableListOf()) { classSymbol ->
            KaBaseResolvedClassTypeQualifier(
                builder.classifierBuilder.buildClassLikeSymbol(classSymbol),
                emptyList(),
            )
        }
    }
}