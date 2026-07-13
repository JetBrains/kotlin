/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * An internal facade that aggregates the Analysis API implementation behind every public
 * `context(session: KaSession)` endpoint.
 *
 * Each capability is exposed as a dedicated `KaInternals<Xxx>` proxy, accessible through a property on this interface. Public endpoints
 * never touch a session implementation directly; they delegate through this facade, for example
 * `session.internals.typeRelationChecker.isSubtypeOf(...)`.
 *
 * [KaInternals] is intentionally **not** a supertype of [KaSession]: [KaSession] stays a clean marker, while session implementations
 * additionally implement [KaInternals]. This is why the [internals] bridge can cast a [KaSession] to [KaInternals].
 *
 * This is an implementation detail with no compatibility guarantees and must not be used outside the Analysis API implementation modules.
 */
@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternals {
    public val analysisScopeProvider: KaInternalsAnalysisScopeProvider

    public val completionCandidateChecker: KaInternalsCompletionCandidateChecker

    public val compilerFacility: KaInternalsCompilerFacility

    public val compilerPluginGeneratedDeclarationsProvider: KaInternalsCompilerPluginGeneratedDeclarationsProvider

    public val dataFlowProvider: KaInternalsDataFlowProvider

    public val diagnosticProvider: KaInternalsDiagnosticProvider

    public val evaluator: KaInternalsEvaluator

    public val expressionInformationProvider: KaInternalsExpressionInformationProvider

    public val expressionTypeProvider: KaInternalsExpressionTypeProvider

    public val javaInteroperabilityComponent: KaInternalsJavaInteroperabilityComponent

    public val kDocProvider: KaInternalsKDocProvider

    public val legacyTypeCreator: KaInternalsTypeCreator

    public val referenceShortener: KaInternalsReferenceShortener

    public val renderer: KaInternalsRenderer

    public val resolveExtensionInfoProvider: KaInternalsResolveExtensionInfoProvider

    public val resolver: KaInternalsResolver

    public val scopeProvider: KaInternalsScopeProvider

    public val signatureSubstitutor: KaInternalsSignatureSubstitutor

    public val sourceProvider: KaInternalsSourceProvider

    public val substitutorProvider: KaInternalsSubstitutorProvider

    public val symbolInformationProvider: KaInternalsSymbolInformationProvider

    public val symbolProvider: KaInternalsSymbolProvider

    public val symbolRelationProvider: KaInternalsSymbolRelationProvider

    public val typeCreatorProvider: KaInternalsTypeCreatorProvider

    public val typeInformationProvider: KaInternalsTypeInformationProvider

    public val typeProvider: KaInternalsTypeProvider

    public val typeRelationChecker: KaInternalsTypeRelationChecker

    public val useSiteModule: KaModule

    public val visibilityChecker: KaInternalsVisibilityChecker
}

/**
 * Accesses the [KaInternals] facade of the [KaSession] context parameter by casting the session to its implementation.
 *
 * This bridge is the single point through which public `context(session: KaSession)` endpoints reach the implementation. It relies on every
 * session implementation also implementing [KaInternals], so the cast always succeeds.
 */
@KaImplementationDetail
context(session: KaSession)
internal val internals: KaInternals
    get() = session as KaInternals
