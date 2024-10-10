/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.getDeprecationForCallSite
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.lookupTracker
import org.jetbrains.kotlin.fir.recordNameLookup
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDeprecated
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeNestedClassAccessedViaInstanceReference
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeCandidateCollector
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

data class QualifierResolutionResult(
    val qualifier: FirResolvedQualifier,
    val applicability: CandidateApplicability,
)

fun BodyResolveComponents.resolveRootPartOfQualifier(
    namedReference: FirSimpleNamedReference,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
    isUsedAsReceiver: Boolean,
): QualifierResolutionResult? {
    val name = namedReference.name
    if (name.asString() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) {
        return buildResolvedQualifierResult(
            qualifiedAccess = qualifiedAccess,
            packageFqName = FqName.ROOT,
            nonFatalDiagnostics = nonFatalDiagnosticsFromExpression,
        )
    }

    val scopes = createCurrentScopeList()
    session.lookupTracker?.recordNameLookup(
        name,
        scopes.asSequence().flatMap { it.scopeOwnerLookupNames }.asIterable(),
        qualifiedAccess.source,
        file.source
    )

    var firstUnsuccessful: FirTypeCandidateCollector.TypeCandidate? = null
    for (scope in scopes) {
        val candidate = scope.getUnambiguousCandidate(name, this) ?: continue
        val symbol = candidate.symbol as? FirClassLikeSymbol ?: continue

        // Only return successful candidates here
        if (candidate.applicability != CandidateApplicability.RESOLVED) {
            if (firstUnsuccessful == null) firstUnsuccessful = candidate
            continue
        }

        return buildResolvedQualifierResultForTopLevelClass(symbol, qualifiedAccess, nonFatalDiagnosticsFromExpression, candidate)
    }

    // If we have only found one unsuccessful candidate, return it.
    if (firstUnsuccessful != null) {
        // We checked the type of the symbol in the loop
        val symbol = firstUnsuccessful.symbol as FirClassLikeSymbol
        return buildResolvedQualifierResultForTopLevelClass(symbol, qualifiedAccess, nonFatalDiagnosticsFromExpression, firstUnsuccessful)
    }

    // KT-72173 To mimic K1 behavior,
    // we allow resolving to classifiers in the root package without import if they are receivers but not top-level.
    return FqName.ROOT.continueQualifierInPackage(
        name,
        qualifiedAccess,
        nonFatalDiagnosticsFromExpression,
        this
    ).takeIf { isUsedAsReceiver || it?.qualifier?.symbol == null }
}

fun FirResolvedQualifier.continueQualifier(
    namedReference: FirSimpleNamedReference,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>,
    session: FirSession,
    components: BodyResolveComponents,
): QualifierResolutionResult? {
    val name = namedReference.name

    // No symbol means it's a package. Continue resolution in that package.
    val outerClassSymbol = symbol ?: return packageFqName.continueQualifierInPackage(
        name,
        qualifiedAccess,
        nonFatalDiagnosticsFromExpression,
        components
    )

    val firClass = outerClassSymbol.fir
    if (firClass !is FirClass) return null

    val nestedClassifierScope =
        firClass.scopeProvider.getNestedClassifierScope(firClass, components.session, components.scopeSession) ?: return null

    session.lookupTracker?.recordNameLookup(
        name,
        nestedClassifierScope.scopeOwnerLookupNames,
        qualifiedAccess.source,
        components.file.source
    )

    val candidate = nestedClassifierScope.getUnambiguousCandidate(name, components) ?: return null
    val nestedClassSymbol = candidate.symbol as? FirClassLikeSymbol ?: return null

    val nonFatalDiagnostics = extractNonFatalDiagnostics(
        qualifiedAccess.source,
        explicitReceiver = null,
        nestedClassSymbol,
        extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
        session
    )

    return components.buildResolvedQualifierResult(
        qualifiedAccess = qualifiedAccess,
        packageFqName = this@continueQualifier.packageFqName,
        relativeClassFqName = this@continueQualifier.relativeClassFqName?.child(name),
        symbol = nestedClassSymbol,
        nonFatalDiagnostics = nonFatalDiagnostics,
        extraTypeArguments = this@continueQualifier.typeArguments,
        candidate = candidate,
        explicitParent = this,
    )
}

private fun FirScope.getUnambiguousCandidate(name: Name, components: BodyResolveComponents): FirTypeCandidateCollector.TypeCandidate? {
    val collector = FirTypeCandidateCollector(components.session, components.file, components.containingDeclarations)
    processClassifiersByName(name, collector::processCandidate)
    return collector.getResult().resolvedCandidateOrNull()
}

private fun FqName.continueQualifierInPackage(
    name: Name,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
    components: BodyResolveComponents,
): QualifierResolutionResult? {
    val childFqName = this.child(name)
    if (components.symbolProvider.getPackage(childFqName) != null) {
        return components.buildResolvedQualifierResult(
            qualifiedAccess = qualifiedAccess,
            packageFqName = childFqName,
            nonFatalDiagnostics = nonFatalDiagnosticsFromExpression,
        )
    }

    val classId = ClassId.topLevel(childFqName)
    val symbol = components.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
    val collector = FirTypeCandidateCollector(components.session, components.file, components.containingDeclarations)
    collector.processCandidate(symbol)
    val candidate = collector.getResult().resolvedCandidateOrNull()

    val nonFatalDiagnostics = extractNonFatalDiagnostics(
        qualifiedAccess.source,
        explicitReceiver = null,
        symbol,
        extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
        components.session
    )
    return components.buildResolvedQualifierResult(
        qualifiedAccess = qualifiedAccess,
        packageFqName = this@continueQualifierInPackage,
        relativeClassFqName = classId.relativeClassName,
        symbol = symbol,
        candidate = candidate,
        nonFatalDiagnostics = nonFatalDiagnostics,
    )
}

private fun BodyResolveComponents.buildResolvedQualifierResultForTopLevelClass(
    symbol: FirClassLikeSymbol<*>,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
    candidate: FirTypeCandidateCollector.TypeCandidate,
): QualifierResolutionResult {
    val classId = symbol.classId
    val nonFatalDiagnostics = extractNonFatalDiagnostics(
        qualifiedAccess.source,
        explicitReceiver = null,
        symbol,
        extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
        session
    )
    return buildResolvedQualifierResult(
        qualifiedAccess = qualifiedAccess,
        packageFqName = classId.packageFqName,
        relativeClassFqName = classId.relativeClassName,
        symbol = symbol,
        nonFatalDiagnostics = nonFatalDiagnostics,
        candidate = candidate,
    )
}

private fun BodyResolveComponents.buildResolvedQualifierResult(
    qualifiedAccess: FirQualifiedAccessExpression,
    packageFqName: FqName,
    relativeClassFqName: FqName? = null,
    symbol: FirClassLikeSymbol<*>? = null,
    nonFatalDiagnostics: List<ConeDiagnostic>? = null,
    extraTypeArguments: List<FirTypeProjection>? = null,
    candidate: FirTypeCandidateCollector.TypeCandidate? = null,
    explicitParent: FirResolvedQualifier? = null,
): QualifierResolutionResult {
    return QualifierResolutionResult(
        buildResolvedQualifierForClass(
            symbol = symbol,
            sourceElement = qualifiedAccess.source,
            packageFqName = packageFqName,
            relativeClassName = relativeClassFqName,
            typeArgumentsForQualifier = qualifiedAccess.typeArguments.applyIf(!extraTypeArguments.isNullOrEmpty()) { plus(extraTypeArguments.orEmpty()) },
            diagnostic = candidate?.diagnostic,
            nonFatalDiagnostics = nonFatalDiagnostics.orEmpty(),
            annotations = qualifiedAccess.annotations,
            explicitParent = explicitParent,
        ),
        candidate?.applicability ?: CandidateApplicability.RESOLVED,
    )
}

internal fun extractNestedClassAccessDiagnostic(
    source: KtSourceElement?,
    explicitReceiver: FirExpression?,
    symbol: FirClassLikeSymbol<*>
): ConeDiagnostic? {
    if ((explicitReceiver?.unwrapSmartcastExpression() as? FirResolvedQualifier)?.typeArguments?.isNotEmpty() == true)
        return ConeNestedClassAccessedViaInstanceReference(source!!, symbol)
    return null
}

internal fun extractNonFatalDiagnostics(
    source: KtSourceElement?,
    explicitReceiver: FirExpression?,
    symbol: FirClassLikeSymbol<*>,
    extraNotFatalDiagnostics: List<ConeDiagnostic>?,
    session: FirSession,
): List<ConeDiagnostic> {
    val prevDiagnostics = (explicitReceiver?.unwrapSmartcastExpression() as? FirResolvedQualifier)?.nonFatalDiagnostics ?: emptyList()
    var result: MutableList<ConeDiagnostic>? = null

    val deprecation = symbol.getDeprecationForCallSite(session)
    if (deprecation != null) {
        result = mutableListOf()
        result.addAll(prevDiagnostics)
        result.add(ConeDeprecated(source, symbol, deprecation))
    }
    if (extraNotFatalDiagnostics != null && extraNotFatalDiagnostics.isNotEmpty()) {
        if (result == null) {
            result = mutableListOf()
            result.addAll(prevDiagnostics)
        }
        result.addAll(extraNotFatalDiagnostics)
    }

    return result?.toList() ?: prevDiagnostics
}
