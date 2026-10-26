/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.components.analysisSession
import org.jetbrains.kotlin.analysis.api.fir.components.toKaSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.resolution.*
import org.jetbrains.kotlin.analysis.api.fir.utils.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPropertyDelegate

private fun FirExpression.asFunctionOperatorCall(
    expectedSourceKind: KtFakeSourceElementKind,
): FirFunctionCall? = (this as? FirFunctionCall)?.takeIf {
    it.origin == FirFunctionCallOrigin.Operator && it.source?.kind == expectedSourceKind
}

context(component: KaFirSessionComponent)
internal fun resolveForLoopCall(firLoop: FirWhileLoop, psi: KtForExpression): KaCallResolutionAttempt? {
    val firHasNextCall = firLoop.condition.asFunctionOperatorCall(KtFakeSourceElementKind.DesugaredForLoop) ?: return null

    val iteratorPropertyAccess = firHasNextCall.explicitReceiver as? FirQualifiedAccessExpression ?: return null
    val iteratorPropertySymbol = (iteratorPropertyAccess.calleeReference as? FirResolvedNamedReference)
        ?.resolvedSymbol as? FirPropertySymbol ?: return null

    val firIteratorCall = iteratorPropertySymbol.fir
        .initializer
        ?.asFunctionOperatorCall(KtFakeSourceElementKind.DesugaredForLoop)
        ?: return null

    val firNextCall = (firLoop.block.statements.firstOrNull() as? FirProperty)
        ?.initializer
        ?.asFunctionOperatorCall(KtFakeSourceElementKind.DesugaredForLoop)
        ?: return null

    val iteratorAttempt = resolveSingleSubCall(firIteratorCall, psi)
    val hasNextAttempt = resolveSingleSubCall(firHasNextCall, psi)
    val nextAttempt = resolveSingleSubCall(firNextCall, psi)

    return KaFirForLoopCallResolutionAttempt(
        backingIteratorCallAttempt = iteratorAttempt,
        backingHasNextCallAttempt = hasNextAttempt,
        backingNextCallAttempt = nextAttempt,
    )
}

context(component: KaFirSessionComponent)
internal fun resolveDelegatedPropertyCall(firProperty: FirProperty, psi: KtPropertyDelegate): KaCallResolutionAttempt? {
    if (firProperty.delegate == null) return null

    val firGetValueCall = (firProperty.getter?.body?.statements?.singleOrNull() as? FirReturnExpression)
        ?.result
        ?.asFunctionOperatorCall(KtFakeSourceElementKind.DelegatedPropertyAccessor.Getter)

    val firSetValueCall = (firProperty.setter?.body?.statements?.singleOrNull() as? FirReturnExpression)
        ?.result
        ?.asFunctionOperatorCall(KtFakeSourceElementKind.DelegatedPropertyAccessor.Setter)

    val firProvideDelegateCall = firProperty.delegate
        ?.asFunctionOperatorCall(KtFakeSourceElementKind.DelegatedPropertyAccessor.DelegateExpression)

    // The getter is mandatory
    if (firGetValueCall == null) return null

    val getterAttempt = resolveSingleSubCall(firGetValueCall, psi)
    val setterAttempt = firSetValueCall?.let { resolveSingleSubCall(it, psi) }
    val provideDelegateAttempt = firProvideDelegateCall?.let { resolveSingleSubCall(it, psi) }

    return KaFirDelegatedPropertyCallResolutionAttempt(
        backingValueGetterCallAttempt = getterAttempt,
        backingValueSetterCallAttempt = setterAttempt,
        backingProvideDelegateCallAttempt = provideDelegateAttempt,
    )
}

/**
 * Resolves a [FirFunctionCall] into a [KaSimpleCallResolutionAttempt].
 * If the call has an error, returns [KaSimpleCallResolutionError]; otherwise builds a [KaSimpleCallResolutionSuccess].
 */
context(component: KaFirSessionComponent)
private fun resolveSingleSubCall(call: FirFunctionCall, psi: KtElement): KaSimpleCallResolutionAttempt {
    findErrorCall(call, psi)?.let { return it }

    return when (val kaCall = buildNamedFunctionCall(call)) {
        null -> KaFirSimpleCallResolutionError(
            backedDiagnostic = KaNonBoundToPsiErrorDiagnostic(
                factoryName = FirErrors.OTHER_ERROR.name,
                defaultMessage = "Failed to build call",
                token = component.token,
            ),
            backingCandidateCalls = emptyList(),
        )

        else -> KaFirSimpleCallResolutionSuccess(backingCall = kaCall)
    }
}

context(component: KaFirSessionComponent)
private fun buildNamedFunctionCall(firFunctionCall: FirFunctionCall): KaFunctionCall<KaNamedFunctionSymbol>? {
    val functionSymbol = (firFunctionCall.calleeReference as? FirResolvedNamedReference)
        ?.resolvedSymbol as? FirNamedFunctionSymbol ?: return null

    val firTypeArgumentsMapping = firFunctionCall.toFirTypeArgumentsMapping(functionSymbol)
    val typeArgumentsMapping = firTypeArgumentsMapping.asKaTypeParametersMapping()

    val kaSignature = with(analysisSession) {
        val substitutor = substitutorByMap(firTypeArgumentsMapping, firSession).toKaSubstitutor()
        functionSymbol.toKaSignature().substitute(substitutor)
    }

    val partiallyAppliedSymbol = KaFirPartiallyAppliedSymbol(
        backingSignature = kaSignature,
        dispatchReceiver = firFunctionCall.dispatchReceiver?.toKaReceiverValue(),
        extensionReceiver = firFunctionCall.extensionReceiver?.toKaReceiverValue(),
        contextArguments = firFunctionCall.contextArguments.toKaContextParameterValues(),
    )

    @Suppress("UNCHECKED_CAST")
    return KaFirSimpleFunctionCall(
        backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
        backingArgumentMapping = emptyMap(),
        backingTypeArgumentsMapping = typeArgumentsMapping,
    ) as KaFunctionCall<KaNamedFunctionSymbol>
}
