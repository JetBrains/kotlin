/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.isDeprecationLevelHidden
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
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.asCone
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.model.safeSubstitute

object ContextSensitiveResolutionReceiverStrategy : ExpectedTypeAsStaticReceiverStrategy<ConeSimpleNameForContextSensitiveResolution> {
    context(resolutionContext: ResolutionContext)
    override fun getClassRepresentative(type: ConeKotlinType): FirRegularClassSymbol? {
        return type.getClassRepresentativeForResolutionByExpectedType(resolutionContext.session)
    }

    context(resolutionContext: ResolutionContext)
    override fun isSuitableReceiver(atom: ConeSimpleNameForContextSensitiveResolution, classSymbol: FirRegularClassSymbol): Boolean {
        // TODO: potentially might have performance cost (KT-89496)
        return resolutionContext.bodyResolveComponents.runContextSensitiveResolutionForSimpleName(
            atom.expression, atom.name, classSymbol
        ) != null
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

    val newExpression =
        context.bodyResolveComponents.runContextSensitiveResolutionForSimpleName(atom.expression, atom.name, classesForResolution)
    val checkerSink = outerCandidateContext.checkerSink ?: CheckerSinkImpl(containingCandidate)

    val atomToCheck = if (newExpression != null) {
        atom.containingCallCandidate.setUpdatedArgumentFromContextSensitiveResolution(atom.expression, newExpression)
        ConeResolutionAtom.createRawAtom(newExpression)
    } else {
        outerCandidateContext.checkerSink?.reportDiagnostic(UnsuccessfulContextSensitiveResolutionArgument)
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
}

/**
 * @return not-nullable value when resolution was successful
 */
fun BodyResolveComponents.runContextSensitiveResolutionForSimpleName(
    originalExpression: FirExpression,
    name: Name,
    expectedType: ConeKotlinType,
): FirExpression? {
    val representativeClass = expectedType.getClassRepresentativeForResolutionByExpectedType(session) ?: return null
    return runContextSensitiveResolutionForSimpleName(originalExpression, name, representativeClass)
}

/**
 * @return not-nullable value when resolution against at least one of the classes was successful,
 * and all the successful results refer to the same declaration.
 */
private fun BodyResolveComponents.runContextSensitiveResolutionForSimpleName(
    originalExpression: FirExpression,
    name: Name,
    representativeClasses: Collection<FirRegularClassSymbol>,
): FirExpression? {
    var result: FirExpression? = null
    for (representativeClass in representativeClasses) {
        val newExpression = runContextSensitiveResolutionForSimpleName(originalExpression, name, representativeClass) ?: continue
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
 * This function is expected to be pure, so it should not modify given expression nor should it change any constraint system.
 *
 * @param originalExpression the result of the regular resolution of the simple name: either a property access or a qualifier.
 *   Its source, annotations and type arguments are used for a new synthetic property access expression.
 * @param name the simple name as it's written in the source
 * @return not-nullable value when resolution was successful
 */
fun BodyResolveComponents.runContextSensitiveResolutionForSimpleName(
    originalExpression: FirExpression,
    name: Name,
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
            typeArguments.addAll(
                when (originalExpression) {
                    is FirQualifiedAccessExpression -> originalExpression.typeArguments
                    is FirResolvedQualifier -> originalExpression.typeArguments
                    else -> error("Unexpected expression type: ${originalExpression::class}")
                }
            )
            explicitReceiver = additionalQualifier
            source = originalExpression.source
            calleeReference = buildSimpleNamedReference {
                source = when (originalExpression) {
                    is FirQualifiedAccessExpression -> originalExpression.calleeReference.source
                    // The same source kind as at RAW FIR, see org.jetbrains.kotlin.fir.builder.ConversionUtilsKt.generateAccessExpression
                    else -> originalExpression.source?.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
                }
                this.name = name
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
                // TODO: Consider using references with candidates, too (KT-89593)
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

/**
 * see [FirPropertyAccessExpression.shouldBeResolvedInContextSensitiveMode].
 */
context(components: BodyResolveComponents)
fun FirResolvedQualifier.shouldBeResolvedInContextSensitiveMode(): Boolean {
    // Only simple name expressions are supported
    if (explicitParent != null) return false
    val qualifierSymbol = qualifierSymbol ?: return false

    if (this is FirErrorResolvedQualifier && this.diagnostic.meansNoAvailableCandidate()) return true

    // A HIDDEN classifier or a class with a HIDDEN companion object has no diagnostic,
    // it's only reported later by FirDeprecatedQualifierChecker
    if (qualifierSymbol.isDeprecationLevelHidden(components.session)) return true
    if (!resolvedToCompanionObject) return false
    val companionSymbol = accessedObjectSymbol ?: return false
    if (companionSymbol.isDeprecationLevelHidden(components.session)) return true

    // TODO: Remove the check if the resolver would mark resolution results to classes with invisible companion objects
    //  with a proper diagnostic (once KT-89656 is fixed), so this case would be handled by the code above.
    companionSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)
    return !components.session.visibilityChecker.isClassLikeVisible(
        companionSymbol.fir,
        components.session,
        components.file,
        components.containingDeclarations
    )
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
