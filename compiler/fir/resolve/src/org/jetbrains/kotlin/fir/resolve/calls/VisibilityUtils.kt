/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionWithSmartcast
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullableNothing
import org.jetbrains.kotlin.fir.types.makeConeTypeDefinitelyNotNullOrNotNull
import org.jetbrains.kotlin.fir.types.typeContext

fun FirVisibilityChecker.isVisible(
    declaration: FirMemberDeclaration,
    callInfo: CallInfo,
    dispatchReceiverValue: ReceiverValue?
): Boolean {
    if (declaration is FirCallableDeclaration && (declaration.isIntersectionOverride || declaration.isSubstitutionOverride)) {
        @Suppress("UNCHECKED_CAST")
        return isVisible(declaration.originalIfFakeOverride() as FirMemberDeclaration, callInfo, dispatchReceiverValue)
    }

    // We won't resolve into the backing field
    // in the first place, if it's not accessible.
    if (declaration is FirBackingField) {
        return true
    }

    val useSiteFile = callInfo.containingFile
    val containingDeclarations = callInfo.containingDeclarations
    val session = callInfo.session

    return isVisible(
        declaration,
        session,
        useSiteFile,
        containingDeclarations,
        dispatchReceiverValue,
        callInfo.callSite is FirVariableAssignment
    )

}

fun FirVisibilityChecker.isVisible(
    declaration: FirMemberDeclaration,
    candidate: Candidate
): Boolean {
    val callInfo = candidate.callInfo

    if (!isVisible(declaration, callInfo, candidate.dispatchReceiverValue)) {
        val dispatchReceiverWithoutSmartCastType =
            removeSmartCastTypeForAttemptToFitVisibility(candidate.dispatchReceiverValue, candidate.callInfo.session) ?: return false

        if (!isVisible(declaration, callInfo, dispatchReceiverWithoutSmartCastType)) return false

        candidate.dispatchReceiverValue = dispatchReceiverWithoutSmartCastType
    }

    val backingField = declaration.getBackingFieldIfApplicable()
    if (backingField != null) {
        candidate.hasVisibleBackingField = isVisible(
            backingField,
            callInfo.session,
            callInfo.containingFile,
            callInfo.containingDeclarations,
            candidate.dispatchReceiverValue,
            candidate.callInfo.callSite is FirVariableAssignment,
        )
    }

    return true
}

private fun removeSmartCastTypeForAttemptToFitVisibility(dispatchReceiverValue: ReceiverValue?, session: FirSession): ReceiverValue? {
    val expressionWithSmartcastIfStable =
        (dispatchReceiverValue?.receiverExpression as? FirExpressionWithSmartcast)?.takeIf { it.isStable } ?: return null

    if (dispatchReceiverValue.type.isNullableNothing) return null

    val originalTypeNotNullable =
        expressionWithSmartcastIfStable.originalType.coneType.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext)

    // Basically, this `if` is just for sake of optimizaton
    // We have only nullability enhancement, here, so return initial smart cast receiver value
    if (originalTypeNotNullable == dispatchReceiverValue.type) return null

    val expressionForReceiver = with(session.typeContext) {
        when {
            expressionWithSmartcastIfStable.originalType.coneType.isNullableType() && !dispatchReceiverValue.type.isNullableType() ->
                buildExpressionWithSmartcast {
                    originalExpression = expressionWithSmartcastIfStable.originalExpression
                    smartcastType =
                        expressionWithSmartcastIfStable.originalExpression.typeRef.resolvedTypeFromPrototype(originalTypeNotNullable)
                    typesFromSmartCast = listOf(originalTypeNotNullable)
                    smartcastStability = expressionWithSmartcastIfStable.smartcastStability
                }
            else -> expressionWithSmartcastIfStable.originalExpression
        }
    }

    return ExpressionReceiverValue(expressionForReceiver)

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
