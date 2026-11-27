/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.ErrorClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeDiagnosticPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeTypePointer
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeTypeRecursionGuard
import org.jetbrains.kotlin.analysis.api.fir.utils.buildAbbreviatedType
import org.jetbrains.kotlin.analysis.api.fir.utils.createPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnmatchedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.renderForDebugging
import org.jetbrains.kotlin.utils.addToStdlib.requireIsInstance

internal class KaFirClassErrorType(
    override val coneType: ConeClassLikeType,
    private val coneDiagnostic: ConeDiagnostic,
    override val analysisSession: KaFirSession,
) : KaClassErrorType(), KaFirType {

    override val qualifiers: List<KaClassTypeQualifier>
        get() = entryPointImplementation {
            when (coneDiagnostic) {
                is ConeUnresolvedError ->
                    ErrorClassTypeQualifierBuilder.createQualifiersForUnresolvedType(coneDiagnostic)
                is ConeUnmatchedTypeArgumentsError ->
                    ErrorClassTypeQualifierBuilder.createQualifiersForUnmatchedTypeArgumentsType(coneDiagnostic)
                else -> error("Unsupported ${coneDiagnostic::class}")
            }
        }

    @KaNonPublicApi
    override val presentableText: String?
        get() = entryPointImplementation {
            qualifiers.joinToString(separator = ".") { it.name.asString() }
        }

    @KaNonPublicApi
    override val errorMessage: String get() = entryPointImplementation { coneDiagnostic.reason }

    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            KaFirAnnotationListForType.create(coneType)
        }

    override val candidateSymbols: Collection<KaClassLikeSymbol>
        get() = entryPointImplementation {
            val symbols = coneDiagnostic.getCandidateSymbols().filterIsInstance<FirClassLikeSymbol<*>>()
            symbols.map { firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(it) }
        }

    override val abbreviation: KaUsualClassType?
        get() = entryPointImplementation {
            buildAbbreviatedType(coneType)
        }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaClassErrorType> = entryPointImplementation {
        return KaFirClassErrorTypePointer(coneType, coneDiagnostic, analysisSession, ConeTypeRecursionGuard())
    }
}

private class KaFirClassErrorTypePointer(
    coneType: ConeClassLikeType,
    coneDiagnostic: ConeDiagnostic,
    analysisSession: KaFirSession,
    guard: ConeTypeRecursionGuard,
) : KaTypePointer<KaClassErrorType> {
    private val coneTypePointer: ConeTypePointer<*> = if (coneType !is ConeErrorType) {
        val classLikeSymbol = analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
        if (classLikeSymbol != null) {
            coneType.createPointer(guard, analysisSession = analysisSession)
        } else {
            val coneErrorType = ConeErrorType(
                ConeUnresolvedSymbolError(coneType.lookupTag.classId),
                isUninferredParameter = false,
                delegatedType = null,
                typeArguments = coneType.typeArguments,
                attributes = coneType.attributes,
                nullable = coneType.isMarkedNullable,
            )
            coneErrorType.createPointer(guard, analysisSession = analysisSession)
        }
    } else {
        coneType.createPointer(guard, analysisSession = analysisSession)
    }

    val coneDiagnosticPointer = ConeDiagnosticPointer.create(coneDiagnostic, guard, analysisSession = analysisSession)

    @KaImplementationDetail
    override fun restore(session: KaSession): KaClassErrorType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val guard = ConeTypeRecursionGuard()
        val coneType = guard.restorePointer(coneTypePointer, analysisSession = session) as? ConeClassLikeType ?: return null
        val coneDiagnostic = coneDiagnosticPointer.restore(analysisSession = session, guard = guard) ?: return null

        return KaFirClassErrorType(coneType, coneDiagnostic, session)
    }
}
