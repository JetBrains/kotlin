/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.util.OperatorNameConventions

fun KtElement.getOrBuildFir(
    phase: FirResolvePhase = FirResolvePhase.BODY_RESOLVE
) = LowLevelFirApiFacade.getOrBuildFirFor(this, phase)


inline fun <reified E : FirElement> KtElement.getOrBuildFirSafe(
    phase: FirResolvePhase = FirResolvePhase.BODY_RESOLVE
) = LowLevelFirApiFacade.getOrBuildFirFor(this, phase) as? E


val KtElement.session
    get() = parentOfType<KtDeclaration>()?.getOrBuildFirSafe<FirDeclaration>()?.session
        ?: containingKtFile.getOrBuildFirSafe<FirFile>()!!.session

fun FirFunctionCall.isImplicitFunctionCall(): Boolean {
    if (dispatchReceiver !is FirQualifiedAccessExpression) return false
    val resolvedCalleeSymbol = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
    return (resolvedCalleeSymbol as? FirNamedFunctionSymbol)?.fir?.name == OperatorNameConventions.INVOKE
}

fun FirFunctionCall.getCalleeSymbol(): FirBasedSymbol<*>? =
    calleeReference.getResolvedSymbolOfNameReference()

fun FirReference.getResolvedSymbolOfNameReference(): FirBasedSymbol<*>? =
    (this as? FirResolvedNamedReference)?.resolvedSymbol