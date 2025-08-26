/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ConeAtomWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ConeCollectionLiteralAtom
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeVisibilityError
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.util.OperatorNameConventions

fun ConeKotlinType.getClassRepresentativeForCollectionLiteralResolution(session: FirSession): FirRegularClassSymbol? {
    return when (this) {
        is ConeLookupTagBasedType ->
            when (val symbol = lookupTag.toSymbol(session)) {
                is FirTypeParameterSymbol, is FirAnonymousObjectSymbol, null -> null
                is FirRegularClassSymbol -> symbol
                is FirTypeAliasSymbol -> fullyExpandedType(session).getClassRepresentativeForCollectionLiteralResolution(session)
            }
        else -> null
    }
}

/**
 * @return if this function is suitable main operator `of` overload, its vararg parameter
 */
context(resolutionContext: ResolutionContext)
fun FirNamedFunctionSymbol.varargParameterOfOperatorOf(outerClass: FirRegularClassSymbol): FirValueParameterSymbol? {
    if (!isOperator || name != OperatorNameConventions.OF) return null
    val varargParameter = valueParameterSymbols.firstOrNull { it.isVararg } ?: return null
    val returnType = resolutionContext.returnTypeCalculator.tryCalculateReturnType(this).coneType
    return if (returnType is ConeClassLikeType && returnType.lookupTag == outerClass.toLookupTag()) varargParameter else null
}

/**
 * @return the overload of operator `of` from the companion object that could be the main overload
 */
context(resolutionContext: ResolutionContext)
fun ConeKotlinType.findMainOperatorOfOverload(): MainOperatorOfOverload? {
    val classSymbol = getClassRepresentativeForCollectionLiteralResolution(resolutionContext.session) ?: return null
    val companionObjectSymbol = classSymbol.resolvedCompanionObjectSymbol ?: return null
    val (varargOverload, varargParameter) = companionObjectSymbol.declarationSymbols.asSequence()
        .filterIsInstance<FirNamedFunctionSymbol>()
        .firstNotNullOfOrNull { declaration ->
            declaration.varargParameterOfOperatorOf(classSymbol)?.let { declaration to it }
        } ?: return null
    return MainOperatorOfOverload(varargOverload, varargParameter, companionObjectSymbol)
}

/**
 * @return always returns a call, even if this call is resolved with errors
 */
fun ResolutionContext.runCollectionLiteralResolution(
    collectionLiteralAtom: ConeCollectionLiteralAtom,
    operatorOf: MainOperatorOfOverload,
    topLevelCandidate: Candidate,
): FirFunctionCall {
    val collectionLiteral = collectionLiteralAtom.expression
    val components = bodyResolveComponents
    val functionCall = buildFunctionCall {
        explicitReceiver =
            operatorOf.companionObjectSymbol.toImplicitResolvedQualifierReceiver(
                components,
                collectionLiteral.source,
                resolvedToCompanion = true,
            )
        source = collectionLiteral.source
        calleeReference = buildSimpleNamedReference {
            source = collectionLiteral.calleeReference.source
            name = OperatorNameConventions.OF
        }
        argumentList = collectionLiteral.argumentList
    }

    val selectedCall =
        components.callResolver.resolveCallAndSelectCandidate(functionCall, ResolutionMode.ContextDependent, topLevelCandidate)
    val completedCall = components.callCompleter.completeCall(selectedCall, ResolutionMode.ContextDependent)

    return when (val calleeRef = completedCall.calleeReference) {
        is FirNamedReferenceWithCandidate -> {
            topLevelCandidate.system.replaceContentWith(calleeRef.candidate.system.currentStorage())
            collectionLiteralAtom.subAtom = ConeAtomWithCandidate(collectionLiteral, calleeRef.candidate)
            completedCall
        }
        else -> completedCall
    }
}

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
