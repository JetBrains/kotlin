/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.ContextSensitiveResolutionMightBeUsed
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
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
            (qualifierSymbol as? FirRegularClassSymbol)?.resolvedCompanionObjectSymbol
        } else {
            qualifierSymbol
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

internal val FirQualifiedAccessExpression.isResolvableByContextSensitiveResolution: Boolean
    get() = nonFatalDiagnostics.any { it is ContextSensitiveResolutionMightBeUsed }

internal val FirResolvedQualifier.isResolvableByContextSensitiveResolution: Boolean
    get() = nonFatalDiagnostics.any { it is ContextSensitiveResolutionMightBeUsed }

internal val FirTypeOperatorCall.isResolvableByContextSensitiveResolution: Boolean
    get() = nonFatalDiagnostics.any { it is ContextSensitiveResolutionMightBeUsed }

/**
 * Retrieves the corresponding [KtUserType] PSI the given [FirResolvedTypeRef].
 *
 * This code handles some quirks of FIR sources and PSI:
 * - in `vararg args: String` declaration, `String` type reference has fake source, but `Array<String>` has real source
 * (see [KtFakeSourceElementKind.ArrayTypeFromVarargParameter]).
 * - if FIR reference points to the type with generic parameters (like `Foo<Bar>`), its source is not [KtTypeReference], but
 * [KtNameReferenceExpression].
 */
internal val FirResolvedTypeRef.correspondingTypePsi: KtUserType?
    get() {
        val sourcePsi = when {
            // array type for vararg parameters is not present in the code, so no need to handle it
            delegatedTypeRef?.source?.kind == KtFakeSourceElementKind.ArrayTypeFromVarargParameter -> null

            // but the array's underlying type is present with a fake source, and needs to be handled
            source?.kind == KtFakeSourceElementKind.ArrayTypeFromVarargParameter -> psi

            else -> realPsi
        }

        val outerTypeElement = when (sourcePsi) {
            is KtTypeReference -> sourcePsi.typeElement
            is KtNameReferenceExpression -> sourcePsi.parent as? KtTypeElement
            else -> null
        }

        return outerTypeElement?.unwrapNullability() as? KtUserType
    }

/**
 * Returns the [FirTypeOperatorCall] (`is`/`!is`/`as`/`as?`, including `when (..) { is .. }`) whose
 * [FirTypeOperatorCall.conversionTypeRef] is [this] — or `null` if [this] is not a conversion type ref
 * of any type-operator call.
 */
internal fun FirResolvedTypeRef.enclosingTypeOperatorCall(resolutionFacade: LLResolutionFacade): FirTypeOperatorCall? {
    val typeReference = correspondingTypePsi?.parentOfType<KtTypeReference>() ?: return null

    val operatorPsi = when (val typeReferenceParent = typeReference.parent) {
        is KtIsExpression -> typeReferenceParent
        is KtBinaryExpressionWithTypeRHS -> typeReferenceParent
        is KtWhenConditionIsPattern -> typeReferenceParent
        else -> return null
    }

    return operatorPsi.getOrBuildFir(resolutionFacade) as? FirTypeOperatorCall
}
