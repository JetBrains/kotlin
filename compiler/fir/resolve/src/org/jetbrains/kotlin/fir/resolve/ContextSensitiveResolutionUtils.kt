/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeVisibilityError
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

fun ConeKotlinType.getClassRepresentativeForContextSensitiveResolution(session: FirSession): FirRegularClassSymbol? {
    return when (this) {
        is ConeFlexibleType ->
            lowerBound.getClassRepresentativeForContextSensitiveResolution(session)?.takeIf {
                it == upperBound.getClassRepresentativeForContextSensitiveResolution(session)
            }

        is ConeDefinitelyNotNullType -> original.getClassRepresentativeForContextSensitiveResolution(session)

        is ConeIntegerLiteralType -> possibleTypes.singleOrNull()?.getClassRepresentativeForContextSensitiveResolution(session)

        is ConeIntersectionType -> {
            val representativesForComponents =
                intersectedTypes.map { it.getClassRepresentativeForContextSensitiveResolution(session) }

            if (representativesForComponents.any { it == null }) return null
            @Suppress("UNCHECKED_CAST") // See the check above
            representativesForComponents as List<FirClassSymbol<*>>

            representativesForComponents.firstOrNull { candidate ->
                representativesForComponents.all { other ->
                    candidate.fir.isSubclassOf(other.toLookupTag(), session, isStrict = false)
                }
            }
        }

        is ConeLookupTagBasedType ->
            when (val symbol = lookupTag.toSymbol(session)) {
                is FirRegularClassSymbol -> symbol

                is FirTypeParameterSymbol ->
                    symbol.resolvedBounds.singleOrNull()?.coneType?.getClassRepresentativeForContextSensitiveResolution(session)

                is FirAnonymousObjectSymbol -> null
                is FirTypeAliasSymbol ->
                    fullyExpandedType(session)
                        .takeIf { it !== this }
                        ?.getClassRepresentativeForContextSensitiveResolution(session)
                null -> null
            }

        is ConeCapturedType, is ConeStubTypeForTypeVariableInSubtyping, is ConeTypeVariableType -> null
    }
}

/**
 * @return not-nullable value when resolution was successful
 */
fun BodyResolveComponents.runContextSensitiveResolutionForPropertyAccess(
    originalExpression: FirPropertyAccessExpression,
    expectedType: ConeKotlinType,
): FirExpression? {
    val representativeClass: FirRegularClassSymbol =
        expectedType.getClassRepresentativeForContextSensitiveResolution(session)
            ?: return null

    val additionalQualifier = buildResolvedQualifier {
        symbol = representativeClass
        packageFqName = representativeClass.classId.packageFqName
        relativeClassFqName = representativeClass.classId.relativeClassName
        source = originalExpression.source?.fakeElement(KtFakeSourceElementKind.QualifierForContextSensitiveResolution)
    }.apply {
        setTypeOfQualifier(this@runContextSensitiveResolutionForPropertyAccess)
    }

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

    return newExpression.takeIf {
        when (it) {
            is FirPropertyAccessExpression -> {
                val newCalleeReference = it.calleeReference
                newCalleeReference is FirResolvedNamedReference && newCalleeReference !is FirResolvedErrorReference
            }

            // resolved qualifiers are always successful when returned
            is FirResolvedQualifier -> true

            // Non-trivial FIR element
            else -> false
        }
    }
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
