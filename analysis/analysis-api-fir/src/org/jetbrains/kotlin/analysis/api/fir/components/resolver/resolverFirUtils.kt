/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import org.jetbrains.kotlin.analysis.api.fir.components.KaFirResolver
import org.jetbrains.kotlin.analysis.api.fir.components.resolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression

/**
 * Some elements require special adjusting on psi or fir level:
 *
 * - For [KtDestructuringDeclarationEntry], [getOrBuildFir] returns [FirProperty] (a declaration).
 *   The actual resolution target is in [FirProperty.initializer] (e.g., [FirComponentCall] or [FirErrorExpression]).
 *
 * - For [KtPropertyDelegate], [getOrBuildFir] should be called on the property to handle the type specially.
 *   The actual resolution target is in [FirProperty.delegate] (e.g., [FirFunctionCall]), which conflicts with the regular logic.
 */
context(resolver: KaFirResolver)
internal fun KtElement.getOrBuildFirWithAdjustments(): FirElement? = when (this) {
    is KtPropertyDelegate -> (parent as? KtElement)?.getOrBuildFir(resolutionFacade)
    else -> when (val fir = getOrBuildFir(resolutionFacade)) {
        is FirProperty if this is KtDestructuringDeclarationEntry -> fir.initializer
        else -> fir
    }
}

/**
 * FIR safe calls may cover more syntax than the corresponding [KtSafeQualifiedExpression]. For example,
 * `s?.itselfFun()["1"]` is represented as `s?.{ $subj$.itselfFun().get("1") }`, even though the PSI safe call
 * is only `s?.itselfFun()`. Resolve the FIR node that corresponds to the requested PSI selector instead of the
 * outer desugared call.
 *
 * If traversal reaches an implicit `invoke`, it is intentionally returned before searching the receiver: for
 * `s?.action()`, resolving the call expression should still resolve to `invoke`, not to the callable expression
 * used as its receiver.
 */
internal fun FirSafeCallExpression.unwrapSelector(): FirElement {
    fun FirElement.findNestedQualifiedAccess(): FirQualifiedAccessExpression? = when (this) {
        is FirSmartCastExpression -> originalExpression.findNestedQualifiedAccess()
        is FirImplicitInvokeCall -> this
        is FirQualifiedAccessExpression -> explicitReceiver?.findNestedQualifiedAccess() ?: this
        else -> null
    }

    return selector.findNestedQualifiedAccess() ?: selector
}
