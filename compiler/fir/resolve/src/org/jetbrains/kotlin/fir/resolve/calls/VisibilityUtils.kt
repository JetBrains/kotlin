/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirVisibilityChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildSmartCastExpression
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.isNullableNothing
import org.jetbrains.kotlin.fir.types.makeConeTypeDefinitelyNotNullOrNotNull
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.utils.addToStdlib.runIf

fun FirVisibilityChecker.isVisible(
    declaration: FirMemberDeclaration,
    callInfo: CallInfo,
    dispatchReceiver: FirExpression?,
    skipCheckForContainingClassVisibility: Boolean = false,
): Boolean {
    val staticQualifierForCallable = runIf(
        declaration is FirCallableDeclaration &&
                declaration.isStatic &&
                isExplicitReceiverExpression(dispatchReceiver)
    ) {
        when (val classLikeSymbol = (dispatchReceiver as? FirResolvedQualifier)?.symbol) {
            is FirRegularClassSymbol -> classLikeSymbol.fir
            is FirTypeAliasSymbol -> classLikeSymbol.fullyExpandedClass(callInfo.session)?.fir
            is FirAnonymousObjectSymbol,
            null -> null
        }
    }
    return isVisible(
        declaration,
        callInfo.session,
        callInfo.containingFile,
        callInfo.containingDeclarations,
        dispatchReceiver,
        staticQualifierClassForCallable = staticQualifierForCallable,
        isCallToPropertySetter = callInfo.callSite is FirVariableAssignment,
        skipCheckForContainingClassVisibility = skipCheckForContainingClassVisibility,
    )
}

fun FirVisibilityChecker.isVisible(
    declaration: FirMemberDeclaration,
    candidate: Candidate,
    skipCheckForContainingClassVisibility: Boolean = false,
): Boolean {
    val callInfo = candidate.callInfo

    if (!isVisible(declaration, callInfo, candidate.dispatchReceiver, skipCheckForContainingClassVisibility)) {
        // There are some examples when applying smart cast makes a callable invisible
        // open class A {
        //     private fun foo() {}
        //     protected open fun bar() {}
        //     fun test(a: A) {
        // !!! foo is visible from a receiver of type A, but not from a receiver of type B
        //         if (a is B) a.foo()
        // !!! B.bar is invisible, but A.bar is visible
        //         if (a is B) a.bar()
        //     }
        // }
        // class B : A() {
        //     override fun bar() {}
        // }
        // In both these examples (see !!! above) we should try to drop smart cast to B and repeat a visibility check
        val dispatchReceiverWithoutSmartCastType =
            removeSmartCastTypeForAttemptToFitVisibility(candidate.dispatchReceiver, candidate.callInfo.session) ?: return false

        if (!isVisible(declaration, callInfo, dispatchReceiverWithoutSmartCastType, skipCheckForContainingClassVisibility)) return false

        // Note: in case of a smart cast, we already checked the visibility of the smart cast target before,
        // so now it's visibility is not important, only callable visibility itself should be taken into account
        // Otherwise we avoid correct smart casts in corner cases with error suppresses like in KT-63164
        // Note 2: ideally this code should be dropped at some time
        // module M1
        // internal class Info {
        //    val status: String = "OK"
        // }
        // module M2(M1)
        // fun getStatus(param: Any?): String {
        //    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        //    if (param is Info) param.status
        // }
        // Here smart cast is still necessary, because without it 'status' cannot be resolved at all
        if (!isVisible(declaration, callInfo, candidate.dispatchReceiver, skipCheckForContainingClassVisibility = true)) {
            candidate.dispatchReceiver = dispatchReceiverWithoutSmartCastType
        }
    }

    val backingField = declaration.getBackingFieldIfApplicable()
    if (backingField != null) {
        candidate.hasVisibleBackingField = isVisible(backingField, callInfo, candidate.dispatchReceiver, skipCheckForContainingClassVisibility)
    }

    return true
}

private fun removeSmartCastTypeForAttemptToFitVisibility(dispatchReceiver: FirExpression?, session: FirSession): FirExpression? {
    val expressionWithSmartcastIfStable =
        (dispatchReceiver as? FirSmartCastExpression)?.takeIf { it.isStable } ?: return null

    val receiverType = dispatchReceiver.resolvedType
    if (receiverType.isNullableNothing) return null

    val originalExpression = expressionWithSmartcastIfStable.originalExpression
    val originalType = originalExpression.resolvedType
    val originalTypeNotNullable =
        originalType.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext)

    // Basically, this `if` is just for sake of optimizaton
    // We have only nullability enhancement, here, so return initial smart cast receiver value
    if (originalTypeNotNullable == receiverType) return null

    val expressionForReceiver = with(session.typeContext) {
        when {
            originalType.isNullableType() && !receiverType.isNullableType() ->
                buildSmartCastExpression {
                    this.originalExpression = originalExpression
                    smartcastType = buildResolvedTypeRef {
                        source = originalExpression.source?.fakeElement(KtFakeSourceElementKind.SmartCastedTypeRef)
                        type = originalTypeNotNullable
                    }
                    typesFromSmartCast = listOf(originalTypeNotNullable)
                    smartcastStability = expressionWithSmartcastIfStable.smartcastStability
                    coneTypeOrNull = originalTypeNotNullable
                }
            else -> originalExpression
        }
    }

    return expressionForReceiver

}

private fun FirMemberDeclaration.getBackingFieldIfApplicable(): FirBackingField? {
    val field = (this as? FirProperty)?.getExplicitBackingField() ?: return null

    // This check prevents resolving protected and
    // public fields.
    return when (field.visibility) {
        Visibilities.PrivateToThis,
        Visibilities.Private,
        Visibilities.Internal -> field
        else -> null
    }
}

private fun isExplicitReceiverExpression(receiverExpression: FirExpression?): Boolean {
    if (receiverExpression == null) return false
    // Only FirThisReference may be a reference in implicit receiver
    @OptIn(UnsafeExpressionUtility::class)
    val thisReference = receiverExpression.toReferenceUnsafe() as? FirThisReference ?: return true
    return !thisReference.isImplicit
}

internal val Candidate.isCodeFragmentVisibilityError
    get() = applicability == CandidateApplicability.K2_VISIBILITY_ERROR &&
            callInfo.containingFile.declarations.singleOrNull() is FirCodeFragment
