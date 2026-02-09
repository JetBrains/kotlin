/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.transformers.appendNonFatalDiagnostics
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

/**
 * @return not-nullable value when resolution was successful
 */
fun BodyResolveComponents.runContextSensitiveResolutionForPropertyAccess(
    originalExpression: FirPropertyAccessExpression,
    expectedType: ConeKotlinType,
): FirExpression? {
    for (representativeClass in expectedType.getParentChainForContextSensitiveResolutionOfExpressions(session)) {
        val additionalQualifier = representativeClass.toImplicitResolvedQualifierReceiver(
            this,
            originalExpression.source?.fakeElement(KtFakeSourceElementKind.QualifierForContextSensitiveResolution)
        )

        val newAccess = buildPropertyAccessExpression {
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
        )


        val shouldTakeNewExpression = when (newExpression) {
            is FirPropertyAccessExpression -> {
                val newCalleeReference = newExpression.calleeReference
                val shouldTake = newCalleeReference is FirResolvedNamedReference && newCalleeReference !is FirResolvedErrorReference
                if (shouldTake) {
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
        else -> false
    }

/**
 * @receiver Resolved version of original FQ name
 */
fun FirQualifierWithContextSensitiveAlternative.appendContextResolutionSensitiveHintIfNeeded(resolvedSimpleNameVersion: FirExpression?): Boolean {
    val originalSymbol = when (this) {
        is FirPropertyAccessExpression -> obtainSymbol()
        is FirResolvedQualifier -> symbol
        else -> error("Unexpected subclass of ${FirQualifierWithContextSensitiveAlternative::class.simpleName}: ${this::class.qualifiedName}")
    } ?: return false

    if (originalSymbol != resolvedSimpleNameVersion?.obtainSymbol()) return false

    when (this) {
        is FirPropertyAccessExpression -> appendNonFatalDiagnostics(ContextSensitiveResolutionMightBeUsed)
        is FirResolvedQualifier -> appendNonFatalDiagnostics(ContextSensitiveResolutionMightBeUsed)
    }

    return true
}

private fun FirExpression.obtainSymbol(): FirBasedSymbol<*>? = when (this) {
    is FirPropertyAccessExpression -> toResolvedCallableSymbol()
    is FirResolvedQualifier -> symbol
    else -> null
}