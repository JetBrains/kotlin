/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolutionAtom
import org.jetbrains.kotlin.fir.resolve.calls.ConeSimpleNameForContextSensitiveResolution
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.UnsuccessfulContextSensitiveResolutionArgument
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSinkImpl
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.stages.ArgumentCheckingProcessor
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.ExpectedTypeAsStaticReceiverStrategy
import org.jetbrains.kotlin.fir.resolve.inference.StateForAtomWithExpectedTypeAsStaticReceiver
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.substitution.asCone
import org.jetbrains.kotlin.fir.resolve.transformers.appendNonFatalDiagnostics
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.asCone
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.model.safeSubstitute

object ContextSensitiveResolutionReceiverStrategy : ExpectedTypeAsStaticReceiverStrategy<ConeSimpleNameForContextSensitiveResolution> {
    context(resolutionContext: ResolutionContext)
    override fun getClassRepresentative(type: ConeKotlinType): FirRegularClassSymbol? {
        return type.getClassRepresentativeForContextSensitiveResolution(resolutionContext.session)
    }

    context(resolutionContext: ResolutionContext)
    override fun isSuitableReceiver(atom: ConeSimpleNameForContextSensitiveResolution, classSymbol: FirRegularClassSymbol): Boolean {
        // TODO: potentially might have performance cost (KT-89496)
        return resolutionContext.bodyResolveComponents.runContextSensitiveResolutionForPropertyAccess(atom.expression, classSymbol) != null
    }
}

/**
 * Resolves the CSR atom and applies the result to the system of an outer candidate:
 * - on success, the resolved expression replaces the original one in the containing call
 * - otherwise, the original (unresolved) expression remains and [UnsuccessfulContextSensitiveResolutionArgument] is reported
 */
context(context: ResolutionContext, outerCandidateContext: OuterCandidateContextForAtomWithExpectedTypeAsStaticReceiver)
fun runContextSensitiveResolutionForAtom(
    state: StateForAtomWithExpectedTypeAsStaticReceiver<ConeSimpleNameForContextSensitiveResolution>,
) {
    val atom = state.atom
    val containingCandidate = outerCandidateContext.containingCandidate
    val csBuilder = containingCandidate.csBuilder
    val substitutedExpectedType = csBuilder.buildCurrentSubstitutor(emptyMap()).asCone()
        .safeSubstitute(csBuilder, atom.expectedType).asCone()

    val classesForResolution: Collection<FirRegularClassSymbol> = when (state) {
        is StateForAtomWithExpectedTypeAsStaticReceiver.SingleBound -> listOf(state.bound)
        is StateForAtomWithExpectedTypeAsStaticReceiver.NonTvExpected -> listOfNotNull(state.bound)
        is StateForAtomWithExpectedTypeAsStaticReceiver.MultipleBounds -> state.bounds
        is StateForAtomWithExpectedTypeAsStaticReceiver.FallbackOnly -> emptyList()
    }

    val newExpression = context.bodyResolveComponents.runContextSensitiveResolutionForPropertyAccess(atom.expression, classesForResolution)
    val checkerSink = outerCandidateContext.checkerSink ?: CheckerSinkImpl(containingCandidate)

    val atomToCheck = if (newExpression != null) {
        atom.containingCallCandidate.setUpdatedArgumentFromContextSensitiveResolution(atom.expression, newExpression)
        ConeResolutionAtom.createRawAtom(newExpression)
    } else {
        atom.fallbackSubAtom
    }

    ArgumentCheckingProcessor.resolveArgumentExpression(
        csBuilder,
        atomToCheck,
        atom.containingCallCandidate,
        substitutedExpectedType,
        checkerSink,
        context = context,
        isReceiver = false,
        isDispatch = false,
    )

    if (newExpression == null) {
        outerCandidateContext.checkerSink?.reportDiagnostic(UnsuccessfulContextSensitiveResolutionArgument)
    }
}

/**
 * @return not-nullable value when resolution was successful
 */
fun BodyResolveComponents.runContextSensitiveResolutionForPropertyAccess(
    originalExpression: FirPropertyAccessExpression,
    expectedType: ConeKotlinType,
): FirExpression? {
    val representativeClass = expectedType.getClassRepresentativeForContextSensitiveResolution(session) ?: return null
    return runContextSensitiveResolutionForPropertyAccess(originalExpression, representativeClass)
}

/**
 * @return not-nullable value when resolution against at least one of the classes was successful,
 * and all the successful results refer to the same declaration.
 */
private fun BodyResolveComponents.runContextSensitiveResolutionForPropertyAccess(
    originalExpression: FirPropertyAccessExpression,
    representativeClasses: Collection<FirRegularClassSymbol>,
): FirExpression? {
    var result: FirExpression? = null
    for (representativeClass in representativeClasses) {
        val newExpression = runContextSensitiveResolutionForPropertyAccess(originalExpression, representativeClass) ?: continue
        if (result == null) {
            result = newExpression
        } else if (result.obtainSymbol() != newExpression.obtainSymbol()) {
            // Different bounds of the expected type variable provide different declarations for the name,
            // there is no way to choose between them
            return null
        }
    }

    return result
}

/**
 * This function is expected to be pure, so it should not modify given property access nor should it change any constraint system.
 * @return not-nullable value when resolution was successful
 */
fun BodyResolveComponents.runContextSensitiveResolutionForPropertyAccess(
    originalExpression: FirPropertyAccessExpression,
    representativeClass: FirRegularClassSymbol,
): FirExpression? {
    for (classToLookAt in representativeClass.getParentChainForContextSensitiveResolution(session, onlySealed = false)) {
        val additionalQualifier = classToLookAt.toImplicitResolvedQualifierReceiver(
            this,
            originalExpression.source?.fakeElement(KtFakeSourceElementKind.QualifierForContextSensitiveResolution),
            definitelyNotCompanion = false,
        )

        val newAccess = buildPropertyAccessExpression {
            annotations.addAll(originalExpression.annotations)
            explicitReceiver = additionalQualifier
            source = originalExpression.source
            calleeReference = buildSimpleNamedReference {
                source = originalExpression.calleeReference.source
                name = originalExpression.calleeReference.name
            }
        }

        val newExpression = callResolver.resolveVariableAccessAndSelectCandidate(
            newAccess,
            isUsedAsReceiver = false, isUsedAsGetClassReceiver = false,
            callSite = newAccess,
            ResolutionMode.ContextIndependent,
            isNestedIntoOuterCallResolution = true,
        )


        val shouldTakeNewExpression = when (newExpression) {
            is FirPropertyAccessExpression -> {
                val newCalleeReference = newExpression.calleeReference
                val shouldTake = newCalleeReference is FirResolvedNamedReference && newCalleeReference !is FirResolvedErrorReference
                if (shouldTake) {
                    if (newExpression.extensionReceiver === additionalQualifier) {
                        // By KEEP, we have to filter here properties with extension receiver
                        if (!isValidContextSensitiveResolutionToExtension(
                                newCalleeReference, newExpression.dispatchReceiver, classToLookAt
                            )
                        ) {
                            return null
                        }
                    }
                    newCalleeReference.replaceResolvedSymbolOrigin(FirResolvedSymbolOrigin.ContextSensitive)
                }
                shouldTake
            }

            // resolved qualifiers are always successful when returned
            is FirResolvedQualifier -> {
                newExpression.replaceResolvedSymbolOrigin(FirResolvedSymbolOrigin.ContextSensitive)
                true
            }

            // Non-trivial FIR element
            else -> false
        }

        if (shouldTakeNewExpression) return newExpression
    }

    return null
}

fun BodyResolveComponents.isValidContextSensitiveResolutionToExtension(
    calleeReference: FirResolvedNamedReference,
    calleeDispatchReceiver: FirExpression?,
    contextRepresentativeClass: FirRegularClassSymbol,
): Boolean {
    val newResolvedSymbol = calleeReference.symbol as? FirPropertySymbol
    // We shouldn't have declaration-site dispatch receiver matched with use-site resolved qualifier
    // (imported from object seems the only case here)
    if (newResolvedSymbol?.dispatchReceiverType != null && calleeDispatchReceiver is FirResolvedQualifier) {
        return false
    }
    // Only properties extending the static and companion object scopes of the contextual types are included
    val receiverOfNewResolvedSymbol =
        (newResolvedSymbol?.receiverParameterSymbol?.resolvedType?.toSymbol() as? FirRegularClassSymbol)?.let {
            if (it.isCompanion) it.getContainingClassSymbol() as? FirRegularClassSymbol else it
        }
    return receiverOfNewResolvedSymbol === contextRepresentativeClass
}

fun FirPropertyAccessExpression.shouldBeResolvedInContextSensitiveMode(): Boolean {
    val diagnostic = when (val calleeReference = calleeReference) {
        is FirErrorNamedReference -> calleeReference.diagnostic
        is FirErrorReferenceWithCandidate -> calleeReference.diagnostic
        is FirResolvedErrorReference -> calleeReference.diagnostic
        else -> return false
    }

    // Only simple name expressions are supported
    if (explicitReceiver != null) return false

    return diagnostic.meansNoAvailableCandidate()
}

private fun ConeDiagnostic.meansNoAvailableCandidate(): Boolean =
    when (this) {
        is ConeUnresolvedError, is ConeVisibilityError, is ConeHiddenCandidateError -> true
        is ConeAmbiguityError -> candidates.all {
            it.applicability == CandidateApplicability.HIDDEN || it.applicability == CandidateApplicability.K2_VISIBILITY_ERROR
        }
        is ConeInapplicableWrongReceiver -> true
        else -> false
    }

/**
 * @receiver Resolved version of original FQ name
 */
fun FirQualifierWithContextSensitiveAlternative.appendCSRAlternativeDiagnosticIfNeeded(resolvedSimpleNameVersion: FirExpression?): Boolean {
    check(this is FirExpression) {
        "All inheritors of sealed FirQualifierWithContextSensitiveAlternative should be expressions, but ${this::class.simpleName} found"
    }

    val symbol = obtainSymbol()
    if (symbol != resolvedSimpleNameVersion?.obtainSymbol()) return false

    if (symbol is FirCallableSymbol<*> && symbol.hadImplicitTypeInSource()) return false

    val diagnostic = when (obtainOrigin()) {
        FirResolvedSymbolOrigin.ExplicitImport, FirResolvedSymbolOrigin.StarImport -> ContextSensitiveResolutionMightBeUsedInsteadOfImport
        else -> ContextSensitiveResolutionMightBeUsed
    }

    when (this) {
        is FirPropertyAccessExpression -> appendNonFatalDiagnostics(diagnostic)
        is FirResolvedQualifier -> appendNonFatalDiagnostics(diagnostic)
    }

    return true
}

private fun FirCallableSymbol<*>.hadImplicitTypeInSource(): Boolean {
    val returnSource = fir.returnTypeRef.source ?: return true
    return returnSource.kind == KtFakeSourceElementKind.ImplicitTypeRef
}

private fun FirExpression.obtainSymbol(): FirBasedSymbol<*>? = when (this) {
    is FirPropertyAccessExpression -> toResolvedCallableSymbol()
    is FirResolvedQualifier -> qualifierSymbol
    else -> null
}

private fun FirExpression.obtainOrigin(): FirResolvedSymbolOrigin? = when (this) {
    is FirPropertyAccessExpression -> (calleeReference as? FirResolvedNamedReference)?.resolvedSymbolOrigin
    is FirResolvedQualifier -> resolvedSymbolOrigin
    else -> null
}
