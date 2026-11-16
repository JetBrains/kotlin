/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSinkImpl
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.stages.ArgumentCheckingProcessor
import org.jetbrains.kotlin.fir.resolve.diagnostics.ContextSensitiveResolutionMightBeUsed
import org.jetbrains.kotlin.fir.resolve.diagnostics.ContextSensitiveResolutionMightBeUsedInsteadOfImport
import org.jetbrains.kotlin.fir.resolve.inference.ExpectedTypeAsStaticReceiverStrategy
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
import org.jetbrains.kotlin.types.model.safeSubstitute
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
            usage = QualifiedAccessUsage.STANDALONE,
            callSite = newAccess,
            ResolutionMode.ContextIndependent,
            isNestedIntoOuterCallResolution = true,
        )

        when (newExpression) {
            is FirPropertyAccessExpression -> {
                val newCalleeReference = newExpression.calleeReference
                // Let's imagine like super & containing classes would be the next tower levels
                if (newCalleeReference.extraTowerLevelsMightTurnItToSuccess) continue

                if (!newExpression.isEligibleForContextSensitiveResolution(newCalleeReference, additionalQualifier, classToLookAt)) {
                    return null
                }
                newCalleeReference.replaceResolvedSymbolOrigin(FirResolvedSymbolOrigin.ContextSensitive)
                return newExpression
            }

            is FirResolvedQualifier -> {
                if (newExpression.extraTowerLevelsMightTurnItToSuccess) continue
                newExpression.replaceResolvedSymbolOrigin(FirResolvedSymbolOrigin.ContextSensitive)
                return newExpression
            }

            // Non-trivial FIR element
            else -> {}
        }
    }

    return null
}

/**
 * @receiver resolved expression with set synthetic receiver (MyExpectedClass.simpleName)
 * @return true when we should accept the resolved expression as a successful CSR candidate.
 * @return false when the resolved expression is not eligible for CSR, so the lookup in the class fails (TODO: review)
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

    // Currently, we don't support references with incomplete/non-trivial inference like FirNamedReferenceWithCandidate
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
    // Only simple name expressions are supported
    if (explicitReceiver != null) return false
    // CSR acts as extra tower levels
    return calleeReference.extraTowerLevelsMightTurnItToSuccess
}

/**
 * see [FirPropertyAccessExpression.shouldBeResolvedInContextSensitiveMode].
 */
context(components: BodyResolveComponents)
fun FirResolvedQualifier.shouldBeResolvedInContextSensitiveMode(): Boolean {
    // Only simple name expressions are supported
    if (explicitParent != null) return false

    if (extraTowerLevelsMightTurnItToSuccess) return true

    // TODO: Return false instead of code below if the resolver would mark resolution results to classes with invisible companion objects
    //  with a proper diagnostic (once KT-89656 is fixed), so this case would be handled by the condition above.
    if (!resolvedToCompanionObject) return false
    val companionSymbol = accessedObjectSymbol ?: return false

    companionSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)
    return !components.session.visibilityChecker.isClassLikeVisible(
        companionSymbol.fir,
        components.session,
        components.file,
        components.containingDeclarations
    )
}

private val FirNamedReference.extraTowerLevelsMightTurnItToSuccess: Boolean
    get() = when (this) {
        is FirErrorReferenceWithCandidate -> wasResolutionTowerExhausted
        is FirErrorNamedReference -> wasResolutionTowerExhausted
        is FirResolvedErrorReference -> wasResolutionTowerExhausted
        else -> false
    }

private val FirResolvedQualifier.extraTowerLevelsMightTurnItToSuccess: Boolean
    get() = this is FirErrorResolvedQualifier && wasResolutionTowerExhausted

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
