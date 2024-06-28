/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.ErrorClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.buildAbbreviatedType
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnmatchedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirClassErrorType(
    override val coneType: ConeClassLikeType,
    private val coneDiagnostic: ConeDiagnostic,
    private val builder: KaSymbolByFirBuilder,
) : KaClassErrorType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token

    override val qualifiers: List<KaClassTypeQualifier> by cached {
        when (coneDiagnostic) {
            is ConeUnresolvedError ->
                ErrorClassTypeQualifierBuilder.createQualifiersForUnresolvedType(coneDiagnostic, builder)
            is ConeUnmatchedTypeArgumentsError ->
                ErrorClassTypeQualifierBuilder.createQualifiersForUnmatchedTypeArgumentsType(coneDiagnostic, builder)
            else -> error("Unsupported ${coneDiagnostic::class}")
        }
    }

    override val nullability: KaTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    @KaAnalysisNonPublicApi
    override val presentableText: String?
        get() = withValidityAssertion {
            qualifiers.joinToString(separator = ".") { it.name.asString() }
        }

    @KaAnalysisNonPublicApi
    override val errorMessage: String get() = withValidityAssertion { coneDiagnostic.reason }

    override val annotations: KaAnnotationList by cached {
        KaFirAnnotationListForType.create(coneType, builder)
    }

    override val candidateSymbols: Collection<KaClassLikeSymbol> by cached {
        val symbols = coneDiagnostic.getCandidateSymbols().filterIsInstance<FirClassLikeSymbol<*>>()
        symbols.map { builder.classifierBuilder.buildClassLikeSymbol(it) }
    }

    override val abbreviatedType: KaUsualClassType? by cached {
        builder.buildAbbreviatedType(coneType)
    }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()
}
