/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ConeAtomWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ConeCollectionLiteralAtom
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

private fun ConeKotlinType.getClassRepresentativeForCollectionLiteralResolution(session: FirSession): FirRegularClassSymbol? {
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

context(resolutionContext: ResolutionContext)
private fun FirCallableSymbol<*>.canBeMainOperatorOfOverload(outerClass: FirRegularClassSymbol): Boolean {
    return when {
        this !is FirNamedFunctionSymbol -> false
        !isOperator || name != OperatorNameConventions.OF || valueParameterSymbols.none { it.isVararg } -> false
        else -> when (val returnType = resolutionContext.returnTypeCalculator.tryCalculateReturnType(this).coneType) {
            is ConeClassLikeType if returnType.fullyExpandedType().lookupTag == outerClass.toLookupTag() -> true
            is ConeErrorType -> true
            else -> false
        }
    }
}

/**
 * @return if there is a suitable operator `of` overload, companion object where it is defined
 */
context(resolutionContext: ResolutionContext)
val ConeKotlinType.companionObjectIfDefinedOperatorOf: FirRegularClassSymbol?
    get() {
        val classSymbol = getClassRepresentativeForCollectionLiteralResolution(resolutionContext.session) ?: return null
        val companionObjectSymbol = classSymbol.resolvedCompanionObjectSymbol ?: return null
        var overloadFound = false
        companionObjectSymbol.processAllDeclaredCallables(resolutionContext.session) { declaration ->
            if (declaration.canBeMainOperatorOfOverload(classSymbol))
                overloadFound = true
        }
        return overloadFound.ifTrue { companionObjectSymbol }
    }

/**
 * @return always returns a call, even if this call is resolved with errors
 */
fun ResolutionContext.runCollectionLiteralResolution(
    collectionLiteralAtom: ConeCollectionLiteralAtom,
    companion: FirRegularClassSymbol,
    topLevelCandidate: Candidate,
): FirFunctionCall {
    val collectionLiteral = collectionLiteralAtom.expression
    val components = bodyResolveComponents
    val functionCall = buildFunctionCall {
        explicitReceiver =
            companion.toImplicitResolvedQualifierReceiver(
                components,
                collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.CompanionObjectForOperatorOfCall),
            )
        source = collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.OperatorOfCall)
        calleeReference = buildSimpleNamedReference {
            source = collectionLiteral.source
            name = OperatorNameConventions.OF
        }
        argumentList = collectionLiteral.argumentList
        origin = FirFunctionCallOrigin.Operator
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




