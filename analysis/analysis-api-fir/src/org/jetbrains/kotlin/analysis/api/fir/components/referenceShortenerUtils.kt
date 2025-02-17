/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

/**
 * Compares [existingCall] and [candidateCall] by their dispatch and extension receivers,
 * and returns true if they match.
 *
 * It allows one to check whether [candidateCall] is actually resolved through same way as [existingCall].
 */
internal fun areReceiversEquivalent(existingCall: FirQualifiedAccessExpression, candidateCall: Candidate): Boolean {
    val existingExtensionSymbol = existingCall.extensionReceiver?.boundSymbolForReceiverExpression()
    val candidateExtensionSymbol = candidateCall.chosenExtensionReceiverExpression()?.boundSymbolForReceiverExpression()

    if (existingExtensionSymbol != candidateExtensionSymbol) return false

    if (resolvesToSameStaticMethods(existingCall, candidateCall)) {
        // no need to compare dispatch receivers for static methods
        return true
    }

    val existingDispatchReceiver = existingCall.dispatchReceiver?.boundSymbolForReceiverExpression()
    val candidateDispatchReceiver = candidateCall.dispatchReceiverExpression()?.boundSymbolForReceiverExpression()

    return existingDispatchReceiver == candidateDispatchReceiver
}

/**
 * Assuming that [FirExpression] represents a receiver/qualifier expression,
 * returns a symbol to which this receiver is bound to.
 *
 * It may be a class, an object, an anonymous function with extension receiver, and so on.
 */
private fun FirExpression.boundSymbolForReceiverExpression(): FirBasedSymbol<*>? = when (this) {
    is FirThisReceiverExpression -> {
        val boundSymbol = calleeReference.boundSymbol
        requireWithAttachment(
            boundSymbol is FirBasedSymbol<*>,
            { "boundSymbol should be ${FirBasedSymbol::class.simpleName}, but actual is ${boundSymbol?.let { it::class.simpleName }}" },
        ) {
            withFirEntry("expression", this@boundSymbolForReceiverExpression)
            withFirEntry("calleeReference", calleeReference)
        }

        boundSymbol
    }

    is FirResolvedQualifier -> {
        if (resolvedToCompanionObject) {
            (symbol as? FirRegularClassSymbol)?.companionObjectSymbol
        } else {
            symbol
        }
    }

    else -> null
}

private fun resolvesToSameStaticMethods(
    existingCall: FirQualifiedAccessExpression,
    candidateCall: Candidate,
): Boolean {
    val existingSymbol = existingCall.calleeReference.symbol ?: return false
    val candidateSymbol = candidateCall.symbol

    if (existingSymbol != candidateSymbol) return false

    val referencedDeclaration = existingSymbol.fir
    return referencedDeclaration is FirMemberDeclaration && referencedDeclaration.isStatic
}

