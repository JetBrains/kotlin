/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.PossiblyFirFakeOverrideSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.reflect.KClass




fun FirFunctionCall.isImplicitFunctionCall(): Boolean {
    if (dispatchReceiver !is FirQualifiedAccessExpression) return false
    val resolvedCalleeSymbol = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
    return (resolvedCalleeSymbol as? FirNamedFunctionSymbol)?.fir?.name == OperatorNameConventions.INVOKE
}

fun FirFunctionCall.getCalleeSymbol(): FirBasedSymbol<*>? =
    calleeReference.getResolvedSymbolOfNameReference()

fun FirReference.getResolvedSymbolOfNameReference(): FirBasedSymbol<*>? =
    (this as? FirResolvedNamedReference)?.resolvedSymbol

internal fun FirReference.getResolvedKtSymbolOfNameReference(builder: KtSymbolByFirBuilder): KtSymbol? =
    (getResolvedSymbolOfNameReference()?.fir as? FirDeclaration)?.let { firDeclaration ->
        builder.buildSymbol(firDeclaration)
    }

internal inline fun <reified D> D.unrollFakeOverrides(): D where D : FirDeclaration, D : FirSymbolOwner<*> {
    val symbol = symbol
    if (symbol !is PossiblyFirFakeOverrideSymbol<*, *>) return this
    if (!symbol.isFakeOrIntersectionOverride) return this
    var current: FirBasedSymbol<*>? = symbol.overriddenSymbol
    while (current is PossiblyFirFakeOverrideSymbol<*, *> && current.isFakeOrIntersectionOverride) {
        current = current.overriddenSymbol
    }
    return current?.fir as D
}

private inline val FirBasedSymbol<*>.isFakeOrIntersectionOverride: Boolean
    get() {
        val origin = (fir as? FirDeclaration)?.origin ?: return false
        return origin == FirDeclarationOrigin.FakeOverride || origin == FirDeclarationOrigin.IntersectionOverride
    }
