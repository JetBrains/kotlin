/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.fir.annotations.fullyExpandedClassId
import org.jetbrains.kotlin.analysis.api.fir.annotations.mapAnnotationParameters
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Checks if the function call is an implicit invoke call with a simple qualified receiver, or looks like it.
 *
 * For example, `foo()` and `foo.bar()` have simple qualified receivers, while `foo!!()`, `{}()` and `(foo ?: bar)()` - don't.
 *
 * @return `true` if the function call has a simple qualified receiver and is an implicit invoke call,
 * or looks like it and resolves to the `invoke` function.
 */
fun FirFunctionCall.isImplicitFunctionCall(): Boolean {
    if (extensionReceiver !is FirQualifiedAccessExpression && dispatchReceiver !is FirQualifiedAccessExpression) return false

    return this is FirImplicitInvokeCall ||
            calleeReference.getCandidateSymbols().any(FirBasedSymbol<*>::isInvokeFunction)
}

/**
 * Returns `true` if the symbol is for a function named `invoke`.
 */
internal fun FirBasedSymbol<*>.isInvokeFunction() =
    (this as? FirNamedFunctionSymbol)?.fir?.name == OperatorNameConventions.INVOKE

fun FirFunctionCall.getCalleeSymbol(): FirBasedSymbol<*>? =
    calleeReference.getResolvedSymbolOfNameReference()

fun FirFunctionCall.getCandidateSymbols(): Collection<FirBasedSymbol<*>> =
    calleeReference.getCandidateSymbols()

fun FirReference.getResolvedSymbolOfNameReference(): FirBasedSymbol<*>? =
    (this as? FirResolvedNamedReference)?.resolvedSymbol

internal fun FirReference.getResolvedKtSymbolOfNameReference(builder: KtSymbolByFirBuilder): KtSymbol? =
    getResolvedSymbolOfNameReference()?.fir?.let(builder::buildSymbol)

internal fun FirErrorNamedReference.getCandidateSymbols(): Collection<FirBasedSymbol<*>> =
    diagnostic.getCandidateSymbols()

internal fun FirNamedReference.getCandidateSymbols(): Collection<FirBasedSymbol<*>> = when (this) {
    is FirResolvedNamedReference -> listOf(resolvedSymbol)
    is FirErrorNamedReference -> getCandidateSymbols()
    else -> emptyList()
}

internal fun ConeDiagnostic.getCandidateSymbols(): Collection<FirBasedSymbol<*>> =
    when (this) {
        is ConeHiddenCandidateError -> {
            // Candidate with @Deprecated(DeprecationLevel.HIDDEN)
            emptyList()
        }
        is ConeDiagnosticWithCandidates -> candidateSymbols
        is ConeDiagnosticWithSymbol<*> -> listOf(symbol)
        else -> emptyList()
    }

internal fun FirAnnotation.toKtAnnotationApplication(useSiteSession: FirSession): KtAnnotationApplication {
    return KtAnnotationApplication(
        fullyExpandedClassId(useSiteSession),
        psi as? KtCallElement,
        useSiteTarget,
        FirAnnotationValueConverter.toNamedConstantValue(
            mapAnnotationParameters(this, useSiteSession),
            useSiteSession,
        )
    )
}
