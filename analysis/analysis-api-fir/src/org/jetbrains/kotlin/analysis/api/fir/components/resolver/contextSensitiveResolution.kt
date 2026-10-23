/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.components.analysisSession
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaContextSensitiveResolutionImportCanBeRemovedImpl
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaContextSensitiveResolutionNotAvailableImpl
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaContextSensitiveResolutionQualifierCanBeRemovedImpl
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaContextSensitiveResolutionUsedImpl
import org.jetbrains.kotlin.analysis.api.resolution.KaContextSensitiveResolutionStatus
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.FirResolvedSymbolOrigin
import org.jetbrains.kotlin.fir.resolve.diagnostics.ContextSensitiveResolutionMightBeUsed
import org.jetbrains.kotlin.fir.resolve.diagnostics.ContextSensitiveResolutionMightBeUsedInsteadOfImport
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

/**
 * Notes on the implementation:
 *
 * ## Invoke calls
 *
 * For expressions like `Bar()`, if `Bar` implicitly points to the companion object,
 * and `()` is an implicit `invoke` operator call, we cannot use [getOrBuildFir]
 * on `Bar` reference to get the corresponding [FirResolvedQualifier].
 *
 * Instead, we have to find the whole [FirImplicitInvokeCall] for the `Bar()` call,
 * and use explicit receiver from there as the correct qualifier.
 *
 * ## Qualified expressions
 *
 * For expressions like `Foo.Bar`, if `Bar` implicitly points to the companion object,
 * there is actually no [FirResolvedQualifier] for the `Foo` alone -
 * only for the whole `Foo.Bar`.
 *
 * So, when you want to check if `Foo` points to the companion object
 * and call [getOrBuildFir] on it, you receive the qualifier for the
 * whole `Foo.Bar`.
 *
 * Fortunately, you cannot have two references implicitly pointing to the
 * companion object in a single dot-qualified expression - only the
 * last reference in the chain can do that.
 *
 * So, if the PSI element of the [KtSimpleNameExpression] and the whole [FirResolvedQualifier]
 * are different, we can certainly say that the [KtSimpleNameExpression] does not
 * point to the companion object.
 */
context(component: KaFirSessionComponent)
internal fun isImplicitReferenceToCompanionImpl(simpleNameExpression: KtSimpleNameExpression): Boolean {
    val implicitInvokeCall = run {
        val parentCallExpression = simpleNameExpression.parent as? KtCallExpression
        parentCallExpression?.getOrBuildFir(analysisSession.resolutionFacade) as? FirImplicitInvokeCall
    }

    val wholeQualifier = implicitInvokeCall?.explicitReceiver
        ?: simpleNameExpression.getOrBuildFir(analysisSession.resolutionFacade)

    return wholeQualifier is FirResolvedQualifier && wholeQualifier.resolvedToCompanionObject
}

context(component: KaFirSessionComponent)
internal fun contextSensitiveResolutionStatusImpl(simpleNameExpression: KtSimpleNameExpression): KaContextSensitiveResolutionStatus {
    val fir = simpleNameExpression.getOrBuildFir(analysisSession.resolutionFacade)
        ?: return KaContextSensitiveResolutionNotAvailableImpl

    if (fir.isResolvedThroughContextSensitiveResolution()) {
        return KaContextSensitiveResolutionUsedImpl
    }

    // The hint is attached to the fully resolved outer node — the whole qualified expression
    // (`Foo.BAR`) or the enclosing type-operator call (`x is Foo.Bar`) — which is not necessarily the
    // FIR mapped to the simple name itself, so it is re-fetched from the appropriate anchor.
    val hintHolder = when (fir) {
        is FirResolvedTypeRef -> simpleNameExpression.enclosingTypeOperatorCall()
        else -> simpleNameExpression.qualifiedExpressionFir() ?: fir
    }

    val nonFatalDiagnostics = when (hintHolder) {
        is FirQualifiedAccessExpression -> hintHolder.nonFatalDiagnostics
        is FirResolvedQualifier -> hintHolder.nonFatalDiagnostics
        is FirTypeOperatorCall -> hintHolder.nonFatalDiagnostics
        else -> emptyList()
    }

    return when {
        nonFatalDiagnostics.isEmpty() -> KaContextSensitiveResolutionNotAvailableImpl
        ContextSensitiveResolutionMightBeUsedInsteadOfImport in nonFatalDiagnostics -> KaContextSensitiveResolutionImportCanBeRemovedImpl
        ContextSensitiveResolutionMightBeUsed in nonFatalDiagnostics -> KaContextSensitiveResolutionQualifierCanBeRemovedImpl
        else -> KaContextSensitiveResolutionNotAvailableImpl
    }
}

context(component: KaFirSessionComponent)
private fun FirElement.isResolvedThroughContextSensitiveResolution(): Boolean = when (this) {
    is FirResolvedTypeRef -> resolvedSymbolOrigin == FirResolvedSymbolOrigin.ContextSensitive
    is FirResolvedQualifier -> resolvedSymbolOrigin == FirResolvedSymbolOrigin.ContextSensitive
    else -> toReference(analysisSession.firSession)?.isContextSensitive == true
}

/**
 * For a [KtSimpleNameExpression] that is the selector of a [KtDotQualifiedExpression], returns the FIR of
 * the whole qualified expression — the node the CSR "removable qualifier/import" hint is attached to.
 *
 * The hint lives on the fully-resolved outer node, not on the FIR mapped to the selector name itself, so
 * it has to be re-fetched from the qualified expression:
 *
 * ```kotlin
 * Foo.BAR // for the `BAR` selector, returns the FIR of the whole `Foo.BAR`
 * ```
 *
 * Returns `null` for anything that is not such a selector (e.g. the `Foo` receiver), so that the caller
 * falls back to the simple name's own FIR.
 */
context(component: KaFirSessionComponent)
private fun KtSimpleNameExpression.qualifiedExpressionFir(): FirElement? {
    return getQualifiedExpressionForSelector()?.getOrBuildFir(analysisSession.resolutionFacade)
}

/**
 * For a [KtSimpleNameExpression] that is the reference of an `is`/`as` operator's (or a `when` `is`-pattern's)
 * conversion type, returns that operator's [FirTypeOperatorCall] — the node the CSR hint is attached to (see
 * [buildTypeOperatorCall][org.jetbrains.kotlin.fir.expressions.builder.buildTypeOperatorCall] in the raw-FIR builder).
 *
 * The hint always concerns the operator's *own* conversion type, so only the name of that type is accepted:
 *
 * ```kotlin
 * b is Base.Child              // `Child` -> the FirTypeOperatorCall; `Base` (qualifier) -> null
 * when (b) { is Base.Child }   // same, for the `when` `is`-pattern
 * b as Base.Child<Foo>         // `Child` -> the call; `Foo` (type argument) -> null
 * ```
 *
 * The accepted PSI shape is `KtUserType` -> `KtTypeReference` (optionally through a `KtNullableType` for
 * `as T?`) -> the operator. A qualifier segment sits under another `KtUserType`, and a name inside a type
 * argument sits under a `KtTypeReference` whose parent is a projection rather than the operator — both
 * therefore yield `null`.
 */
context(component: KaFirSessionComponent)
private fun KtSimpleNameExpression.enclosingTypeOperatorCall(): FirTypeOperatorCall? {
    val userType = parent as? KtUserType ?: return null
    if (userType.referenceExpression != this) return null

    val typeReferenceParent = userType.parent.let { if (it is KtNullableType) it.parent else it } as? KtTypeReference ?: return null
    return when (val operator = typeReferenceParent.parent) {
        is KtBinaryExpressionWithTypeRHS, is KtIsExpression, is KtWhenConditionIsPattern ->
            operator.getOrBuildFir(analysisSession.resolutionFacade) as? FirTypeOperatorCall

        else -> null
    }
}

internal val FirReference.isContextSensitive: Boolean
    get() = this is FirResolvedNamedReference && resolvedSymbolOrigin == FirResolvedSymbolOrigin.ContextSensitive
