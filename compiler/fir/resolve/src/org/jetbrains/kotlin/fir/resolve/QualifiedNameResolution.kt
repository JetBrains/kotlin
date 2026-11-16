/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeNestedClassAccessedViaInstanceReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeQualifierNotResolvedAsValue
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
import org.jetbrains.kotlin.utils.addToStdlib.runIf

data class QualifierResolutionResult(
    val qualifier: FirResolvedQualifier,
    val applicability: CandidateApplicability,
)

/**
 * Encodes the status of a qualified access resolution as a value (in contrast to it being an explicit receiver)
 */
enum class StandaloneQualifiedAccessAsValueFailure {
    /** No failure: it's either used as a receiver, or the tower resolver has resolved it to a value */
    NONE,

    /**
     * The value is standalone and the basic resolution failed with all tower levels exhausted.
     * See [FirErrorResolvedQualifier.wasResolutionTowerExhausted]
     */
    WITH_TOWER_EXHAUSTION,

    /**
     * The value is standalone and the basic resolution failed, but has stopped at some tower level.
     */
    NO_TOWER_EXHAUSTION,
}

fun BodyResolveComponents.resolveRootPartOfQualifier(
    namedReference: FirSimpleNamedReference,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
    isUsedAsReceiver: Boolean,
    standaloneValueFailure: StandaloneQualifiedAccessAsValueFailure,
): QualifierResolutionResult? {
    val name = namedReference.name
    if (FirQualifierResolver.isRootIdePackageAllowed() && name.asString() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) {
        return buildResolvedQualifierResult(
            qualifiedAccess = qualifiedAccess,
            packageFqName = FqName.ROOT,
            nonFatalDiagnostics = nonFatalDiagnosticsFromExpression,
            resolvedSymbolOrigin =
                if (FirQualifierResolver.isRootIdePackageDeprecated())
                    FirResolvedSymbolOrigin.QualifiedWithDeprecatedRootIdePackage
                else
                    FirResolvedSymbolOrigin.Qualified
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

        return buildResolvedQualifierResultForTopLevelClass(
            symbol, qualifiedAccess, nonFatalDiagnosticsFromExpression, candidate, standaloneValueFailure
        )
    }

    // If we have only found one unsuccessful candidate, return it.
    if (firstUnsuccessful != null) {
        // We checked the type of the symbol in the loop
        val symbol = firstUnsuccessful.symbol as FirClassLikeSymbol
        return buildResolvedQualifierResultForTopLevelClass(
            symbol, qualifiedAccess, nonFatalDiagnosticsFromExpression, firstUnsuccessful, standaloneValueFailure
        )
    }

    // KT-72173 To mimic K1 behavior,
    // we allow resolving to classifiers in the root package without import if they are receivers but not top-level.
    return continueQualifierInPackage(
        FqName.ROOT,
        name,
        qualifiedAccess,
        nonFatalDiagnosticsFromExpression,
        explicitParent = null,
    ).takeIf { isUsedAsReceiver || it?.qualifier?.qualifierSymbol == null }
}

fun FirResolvedQualifier.continueQualifier(
    namedReference: FirSimpleNamedReference,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>,
    session: FirSession,
    components: BodyResolveComponents,
    standaloneValueFailure: StandaloneQualifiedAccessAsValueFailure,
): QualifierResolutionResult? {
    val name = namedReference.name

    val resolvedSymbolOriginForNextQualifierPart =
        (qualifiedAccess.explicitReceiver as? FirResolvedQualifier)?.resolvedSymbolOrigin
            ?.takeIf { it == FirResolvedSymbolOrigin.QualifiedWithDeprecatedRootIdePackage }
            ?: FirResolvedSymbolOrigin.Qualified

    // No symbol means it's a package. Continue resolution in that package.
    val outerClassSymbol = qualifierSymbol ?: return components.continueQualifierInPackage(
        packageFqName,
        name,
        qualifiedAccess,
        nonFatalDiagnosticsFromExpression,
        explicitParent = this,
        resolvedSymbolOriginForNextQualifierPart,
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

    val candidate = nestedClassifierScope.getUnambiguousCandidate(name, components, resolvedSymbolOriginForNextQualifierPart) ?: return null
    val nestedClassSymbol = candidate.symbol as? FirClassLikeSymbol ?: return null

    val nonFatalDiagnostics = extractNonFatalDiagnostics(
        explicitReceiver = null,
        extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
    )

    return components.buildResolvedQualifierResult(
        qualifiedAccess = qualifiedAccess,
        packageFqName = this@continueQualifier.packageFqName,
        relativeClassFqName = this@continueQualifier.relativeClassFqName?.child(name),
        symbol = nestedClassSymbol,
        nonFatalDiagnostics = nonFatalDiagnostics,
        extraTypeArguments = this@continueQualifier.typeArguments,
        candidate = candidate,
        explicitParent = this@continueQualifier,
        standaloneValueFailure = standaloneValueFailure,
    )
}

private fun FirScope.getUnambiguousCandidate(
    name: Name,
    components: BodyResolveComponents,
    resolvedSymbolOrigin: FirResolvedSymbolOrigin? = null,
): FirTypeCandidateCollector.TypeCandidate? {
    val collector = FirTypeCandidateCollector(components.session, components.file, components.containingDeclarations)
    processClassifiersByName(name) {
        collector.processCandidate(it, null, resolvedSymbolOrigin ?: this@getUnambiguousCandidate.toResolvedSymbolOrigin())
    }
    return collector.getResult().resolvedCandidateOrNull()
}

private fun BodyResolveComponents.continueQualifierInPackage(
    parentFqName: FqName,
    name: Name,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
    explicitParent: FirResolvedQualifier?,
    resolvedSymbolOrigin: FirResolvedSymbolOrigin = FirResolvedSymbolOrigin.Qualified,
): QualifierResolutionResult? {
    val childFqName = parentFqName.child(name)
    if (qualifiedAccess.canBePackagePartOfQualifier() && symbolProvider.hasPackage(childFqName)) {
        return buildResolvedQualifierResult(
            qualifiedAccess = qualifiedAccess,
            packageFqName = childFqName,
            nonFatalDiagnostics = nonFatalDiagnosticsFromExpression,
            resolvedSymbolOrigin = resolvedSymbolOrigin,
        )
    }

    val classId = ClassId.topLevel(childFqName)
    val symbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
    session.lookupTracker?.recordClassLikeLookup(classId, qualifiedAccess.source, file.source)
    val collector = FirTypeCandidateCollector(session, file, containingDeclarations)
    collector.processCandidate(
        symbol,
        resolvedSymbolOrigin = if (annotatedPackageQualifiersForbidden) FirResolvedSymbolOrigin.Qualified else resolvedSymbolOrigin,
    )
    val candidate = collector.getResult().resolvedCandidateOrNull()

    val nonFatalDiagnostics = extractNonFatalDiagnostics(
        explicitReceiver = null,
        extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
    )
    return buildResolvedQualifierResult(
        qualifiedAccess = qualifiedAccess,
        packageFqName = parentFqName,
        relativeClassFqName = classId.relativeClassName,
        symbol = symbol,
        candidate = candidate,
        nonFatalDiagnostics = nonFatalDiagnostics,
        explicitParent = explicitParent.takeIf { annotatedPackageQualifiersForbidden },
    )
}

private fun BodyResolveComponents.buildResolvedQualifierResultForTopLevelClass(
    symbol: FirClassLikeSymbol<*>,
    qualifiedAccess: FirQualifiedAccessExpression,
    nonFatalDiagnosticsFromExpression: List<ConeDiagnostic>?,
    candidate: FirTypeCandidateCollector.TypeCandidate,
    standaloneValueFailure: StandaloneQualifiedAccessAsValueFailure,
): QualifierResolutionResult {
    val classId = symbol.classId
    val nonFatalDiagnostics = extractNonFatalDiagnostics(
        explicitReceiver = null,
        extraNotFatalDiagnostics = nonFatalDiagnosticsFromExpression,
    )
    return buildResolvedQualifierResult(
        qualifiedAccess = qualifiedAccess,
        packageFqName = classId.packageFqName,
        relativeClassFqName = classId.relativeClassName,
        symbol = symbol,
        nonFatalDiagnostics = nonFatalDiagnostics,
        candidate = candidate,
        standaloneValueFailure = standaloneValueFailure,
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
    resolvedSymbolOrigin: FirResolvedSymbolOrigin? = null,
    standaloneValueFailure: StandaloneQualifiedAccessAsValueFailure = StandaloneQualifiedAccessAsValueFailure.NONE,
): QualifierResolutionResult {
    return QualifierResolutionResult(
        buildResolvedQualifierForClass(
            symbol = symbol,
            sourceElement = qualifiedAccess.source,
            packageFqName = packageFqName,
            relativeClassName = relativeClassFqName,
            typeArgumentsForQualifier = qualifiedAccess.typeArguments.applyIf(!extraTypeArguments.isNullOrEmpty()) { plus(extraTypeArguments.orEmpty()) },
            diagnostic = candidate?.diagnostic
                ?: runIf(standaloneValueFailure != StandaloneQualifiedAccessAsValueFailure.NONE && symbol != null) {
                    ConeQualifierNotResolvedAsValue
                },
            nonFatalDiagnostics = nonFatalDiagnostics.orEmpty(),
            annotations = qualifiedAccess.annotations,
            explicitParent = explicitParent,
            resolvedSymbolOrigin = candidate?.resolvedSymbolOrigin ?: resolvedSymbolOrigin,
            wasResolutionTowerExhausted = standaloneValueFailure == StandaloneQualifiedAccessAsValueFailure.WITH_TOWER_EXHAUSTION,
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
    explicitReceiver: FirExpression?,
    extraNotFatalDiagnostics: List<ConeDiagnostic>?
): List<ConeDiagnostic> {
    val prevDiagnostics = (explicitReceiver?.unwrapSmartcastExpression() as? FirResolvedQualifier)?.nonFatalDiagnostics ?: emptyList()
    return when {
        !extraNotFatalDiagnostics.isNullOrEmpty() -> prevDiagnostics + extraNotFatalDiagnostics
        else -> prevDiagnostics
    }
}

/**
 * ```kotlin
 * package part1.part2.part3
 *
 * fun foo() { }
 * ```
 * If the user wrote something like `part1.part2<Int>.part3.foo()` it is generally unclear what they wanted.
 *
 * This function forces (when the feature is on) for `part2` to become an unresolved `FirPropertyAccessExpression`.
 * The reason for this is that we have better control over unresolved property accesses: we do not have a `FirResolvedQualifier` for each
 * package part which means we may drop annotations and type arguments on these part from FIR (which is bad for AA).
 */
context(sessionHolder: SessionHolder)
private fun FirQualifiedAccessExpression.canBePackagePartOfQualifier(): Boolean {
    if (!annotatedPackageQualifiersForbidden) {
        return true
    }

    return annotations.isEmpty() && typeArguments.isEmpty()
}

context(sessionHolder: SessionHolder)
private val annotatedPackageQualifiersForbidden: Boolean
    get() = LanguageFeature.ForbidAnnotationsTypeArgumentsAndParenthesesForPackageQualifier.isEnabled()
