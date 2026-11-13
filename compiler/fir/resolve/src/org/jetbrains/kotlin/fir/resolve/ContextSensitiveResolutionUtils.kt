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
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolutionAtom
import org.jetbrains.kotlin.fir.resolve.calls.ConeSimpleNameForContextSensitiveResolution
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.UnsuccessfulContextSensitiveResolutionArgument
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSinkImpl
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.stages.ArgumentCheckingProcessor
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.ExpectedTypeAsStaticReceiverStrategy
import org.jetbrains.kotlin.fir.resolve.inference.ReceiverSuitability
import org.jetbrains.kotlin.fir.resolve.inference.StateForAtomWithExpectedTypeAsStaticReceiver
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.substitution.asCone
import org.jetbrains.kotlin.fir.resolve.transformers.appendNonFatalDiagnostics
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.asCone
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.model.safeSubstitute
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object ContextSensitiveResolutionReceiverStrategy : ExpectedTypeAsStaticReceiverStrategy<ConeSimpleNameForContextSensitiveResolution> {
    context(resolutionContext: ResolutionContext)
    override fun getClassRepresentative(type: ConeKotlinType): FirRegularClassSymbol? {
        return type.getClassRepresentativeForResolutionByExpectedType(resolutionContext.session)
    }

    context(resolutionContext: ResolutionContext)
    override fun getReceiverSuitability(
        atom: ConeSimpleNameForContextSensitiveResolution,
        classSymbol: FirRegularClassSymbol,
    ): ReceiverSuitability {
        // TODO: potentially might have performance cost (KT-89496)
        val result = resolutionContext.bodyResolveComponents.runContextSensitiveResolutionForSimpleNameInClass(
            atom.expression, atom.name, classSymbol
        )
        return when (result) {
            is Resolved -> SUITABLE
            NotFound -> NOT_SUITABLE
            Blocked -> BLOCKING
        }
    }
}

/**
 * The result of the context-sensitive resolution of a simple name against a single contextual class.
 */
private sealed class ContextSensitiveResolutionResult {
    class Resolved(val expression: FirExpression) : ContextSensitiveResolutionResult()

    /**
     * Neither the class nor its sealed parents provide a declaration for the name, so other contextual classes may be tried.
     */
    object NotFound : ContextSensitiveResolutionResult()

    /**
     * The regular resolution of the name against the class has met some candidate, but it has either failed
     * or resolved to a declaration that is not eligible for CSR, thus the whole context-sensitive resolution should fail.
     */
    object Blocked : ContextSensitiveResolutionResult()
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
    return runContextSensitiveResolutionForSimpleName(originalExpression, name, listOf(representativeClass))
}

/**
 * @return not-nullable value when resolution against at least one of the classes was successful,
 * all the successful results refer to the same declaration, and none of the classes blocks the resolution.
 */
private fun BodyResolveComponents.runContextSensitiveResolutionForSimpleName(
    originalExpression: FirExpression,
    name: Name,
    representativeClasses: Collection<FirRegularClassSymbol>,
): FirExpression? {
    var result: FirExpression? = null
    for (representativeClass in representativeClasses) {
        val newExpression =
            when (val classResult = runContextSensitiveResolutionForSimpleNameInClass(originalExpression, name, representativeClass)) {
                is Resolved -> classResult.expression
                NotFound -> continue
                Blocked -> return null
            }
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
 */
private fun BodyResolveComponents.runContextSensitiveResolutionForSimpleNameInClass(
    originalExpression: FirExpression,
    name: Name,
    representativeClass: FirRegularClassSymbol,
): ContextSensitiveResolutionResult {
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

        when (newExpression) {
            is FirPropertyAccessExpression -> {
                val newCalleeReference = newExpression.calleeReference
                when {
                    // No candidate at this class at all, the parent chain may still provide one
                    newCalleeReference.diagnosticOrNull?.meansNoAvailableCandidate() == true -> continue
                    // Any failure of the regular resolution (ambiguity, inapplicability),
                    // a non-trivial candidate needed further completion,
                    // or a candidate with non-trivial receiver configuration mean that CSR is blocked.
                    !newExpression.isEligibleForContextSensitiveResolution(
                        newCalleeReference, additionalQualifier, classToLookAt
                    ) ->
                        return ContextSensitiveResolutionResult.Blocked
                    else -> {
                        newCalleeReference.replaceResolvedSymbolOrigin(FirResolvedSymbolOrigin.ContextSensitive)
                        return ContextSensitiveResolutionResult.Resolved(newExpression)
                    }
                }
            }

            // resolved qualifiers are always successful when returned
            is FirResolvedQualifier -> {
                newExpression.replaceResolvedSymbolOrigin(FirResolvedSymbolOrigin.ContextSensitive)
                return ContextSensitiveResolutionResult.Resolved(newExpression)
            }

            // Non-trivial FIR element, usually that should not happen for x.y qualified expression,
            // but I'm not ready yet to add an assertion here.
            else -> return ContextSensitiveResolutionResult.Blocked
        }
    }

    return ContextSensitiveResolutionResult.NotFound
}

/**
 * @receiver resolved expression with set synthetic receiver (MyExpectedClass.simpleName)
 * @return true when we should accept the resolved expression as a successful CSR candidate.
 * @return false when the resolved expression is not eligible for CSR _and_ we should prevent context-sensitive resolution at all.
 */
@OptIn(ExperimentalContracts::class)
context(components: BodyResolveComponents)
private fun FirPropertyAccessExpression.isEligibleForContextSensitiveResolution(
    resolvedCalleeReference: FirNamedReference,
    additionalQualifier: FirResolvedQualifier,
    contextRepresentativeClass: FirRegularClassSymbol,
): Boolean {
    contract {
        returns(true) implies (resolvedCalleeReference is FirResolvedNamedReference)
    }

    // The regular resolution has chosen a candidate but has failed on it, e.g. an inapplicable one.
    // At the same time, at this point we checked that at least some visible candidate has been found,
    // thus we shouldn't continue looking forward.
    if (resolvedCalleeReference !is FirResolvedNamedReference || resolvedCalleeReference is FirResolvedErrorReference) return false

    val symbol = resolvedCalleeReference.resolvedSymbol as? FirCallableSymbol<*> ?: return false
    // Properties with context parameters are not eligible for CSR
    if (symbol.hasContextParameters) return false
    // By KEEP, we have to filter out some properties with extension receiver
    extensionReceiver?.let {
        if (!hasEligibleExtensionReceiver(it, symbol, additionalQualifier, contextRepresentativeClass)) return false
    }

    // members of the companion object and of the static scope are always eligible
    return true
}

context(components: BodyResolveComponents)
private fun FirPropertyAccessExpression.hasEligibleExtensionReceiver(
    extensionReceiver: FirExpression,
    symbol: FirCallableSymbol<*>,
    additionalQualifier: FirResolvedQualifier,
    contextRepresentativeClass: FirRegularClassSymbol,
): Boolean {
    // The qualifier is the only possible extension receiver here: a member extension of the companion object
    // with an implicit extension receiver is not even found with an explicit qualifier as the dispatch receiver
    if (extensionReceiver !== additionalQualifier) return false
    // We shouldn't have declaration-site dispatch receiver matched with use-site resolved qualifier
    // (imported from object seems the only case here)
    if (symbol.dispatchReceiverType != null && dispatchReceiver is FirResolvedQualifier) {
        return false
    }
    // Only properties extending the static and companion object scopes of the contextual types are included
    val receiverOfNewResolvedSymbol =
        (symbol.receiverParameterSymbol?.resolvedType?.toSymbol() as? FirRegularClassSymbol)?.let {
            if (it.isCompanion) it.getContainingClassSymbol() as? FirRegularClassSymbol else it
        }
    return receiverOfNewResolvedSymbol === contextRepresentativeClass
}

fun FirPropertyAccessExpression.shouldBeResolvedInContextSensitiveMode(): Boolean {
    val diagnostic = calleeReference.diagnosticOrNull ?: return false
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
    if (!resolvedToCompanionObject) {
        // CSR is needed when NO_COMPANION_OBJECT would be reported otherwise
        return accessedObjectSymbol == null
    }
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

private val FirReference.diagnosticOrNull: ConeDiagnostic?
    get() = when (this) {
        is FirErrorNamedReference -> diagnostic
        is FirErrorReferenceWithCandidate -> diagnostic
        is FirResolvedErrorReference -> diagnostic
        else -> null
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
