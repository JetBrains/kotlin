/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.ErrorClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeDiagnosticPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeTypePointer
import org.jetbrains.kotlin.analysis.api.fir.utils.buildAbbreviatedType
import org.jetbrains.kotlin.analysis.api.fir.utils.createPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnmatchedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirClassErrorType(
    override val coneType: ConeClassLikeType,
    private val coneDiagnostic: ConeDiagnostic,
    private val builder: KaSymbolByFirBuilder,
) : KaClassErrorType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token

    override val qualifiers: List<KaClassTypeQualifier>
        get() = withValidityAssertion {
            when (coneDiagnostic) {
                is ConeUnresolvedError ->
                    ErrorClassTypeQualifierBuilder.createQualifiersForUnresolvedType(coneDiagnostic, builder)
                is ConeUnmatchedTypeArgumentsError ->
                    ErrorClassTypeQualifierBuilder.createQualifiersForUnmatchedTypeArgumentsType(coneDiagnostic, builder)
                else -> error("Unsupported ${coneDiagnostic::class}")
            }
        }

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    override val nullability: KaTypeNullability
        get() = withValidityAssertion {
            val coneType = coneType
            if (coneType is ConeErrorType) {
                coneType.nullable?.let(KaTypeNullability::create) ?: KaTypeNullability.UNKNOWN
            } else {
                KaTypeNullability.create(coneType.isMarkedNullable)
            }
        }

    @KaNonPublicApi
    override val presentableText: String?
        get() = withValidityAssertion {
            qualifiers.joinToString(separator = ".") { it.name.asString() }
        }

    @KaNonPublicApi
    override val errorMessage: String get() = withValidityAssertion { coneDiagnostic.reason }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForType.create(coneType, builder)
        }

    override val candidateSymbols: Collection<KaClassLikeSymbol>
        get() = withValidityAssertion {
            val symbols = coneDiagnostic.getCandidateSymbols().filterIsInstance<FirClassLikeSymbol<*>>()
            symbols.map { builder.classifierBuilder.buildClassLikeSymbol(it) }
        }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion {
            builder.buildAbbreviatedType(coneType)
        }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaClassErrorType> = withValidityAssertion {
        return KaFirClassErrorTypePointer(coneType, coneDiagnostic, builder)
    }
}

private class KaFirClassErrorTypePointer(
    coneType: ConeClassLikeType,
    coneDiagnostic: ConeDiagnostic,
    builder: KaSymbolByFirBuilder,
) : KaTypePointer<KaClassErrorType> {
    private val coneTypePointer: ConeTypePointer<*> = if (coneType !is ConeErrorType) {
        val classSymbol = builder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
        if (classSymbol != null) {
            coneType.createPointer(builder)
        } else {
            val coneErrorType = ConeErrorType(
                ConeUnresolvedSymbolError(coneType.lookupTag.classId),
                isUninferredParameter = false,
                delegatedType = null,
                typeArguments = coneType.typeArguments,
                attributes = coneType.attributes,
                nullable = coneType.isMarkedNullable,
            )
            coneErrorType.createPointer(builder)
        }
    } else {
        coneType.createPointer(builder)
    }

    val coneDiagnosticPointer = ConeDiagnosticPointer.create(coneDiagnostic, builder)

    @KaImplementationDetail
    override fun restore(session: KaSession): KaClassErrorType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val coneType = coneTypePointer.restore(session) as? ConeClassLikeType ?: return null
        val coneDiagnostic = coneDiagnosticPointer.restore(session) ?: return null

        return KaFirClassErrorType(coneType, coneDiagnostic, session.firSymbolBuilder)
    }
}