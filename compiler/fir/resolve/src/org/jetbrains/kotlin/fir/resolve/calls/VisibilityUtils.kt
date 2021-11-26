/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirVisibilityChecker
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.isIntersectionOverride
import org.jetbrains.kotlin.fir.isSubstitutionOverride
import org.jetbrains.kotlin.fir.originalIfFakeOverride

fun FirVisibilityChecker.isVisible(
    declaration: FirMemberDeclaration,
    candidate: Candidate
): Boolean {
    if (declaration is FirCallableDeclaration && (declaration.isIntersectionOverride || declaration.isSubstitutionOverride)) {
        @Suppress("UNCHECKED_CAST")
        return isVisible(declaration.originalIfFakeOverride() as FirMemberDeclaration, candidate)
    }

    val callInfo = candidate.callInfo
    val useSiteFile = callInfo.containingFile
    val containingDeclarations = callInfo.containingDeclarations
    val session = callInfo.session

    // We won't resolve into the backing field
    // in the first place, if it's not accessible.
    if (declaration is FirBackingField) {
        return true
    }

    val visible = isVisible(
        declaration,
        session,
        useSiteFile,
        containingDeclarations,
        candidate.dispatchReceiverValue,
        candidate.callInfo.callSite is FirVariableAssignment
    )
    val backingField = declaration.getBackingFieldIfApplicable()

    if (visible && backingField != null) {
        candidate.hasVisibleBackingField = isVisible(
            backingField,
            session,
            useSiteFile,
            containingDeclarations,
            candidate.dispatchReceiverValue,
            candidate.callInfo.callSite is FirVariableAssignment,
        )
    }

    return visible
}

private fun FirMemberDeclaration.getBackingFieldIfApplicable(): FirBackingField? {
    val field = (this as? FirProperty)?.getExplicitBackingField() ?: return null

    // This check prevents resolving protected and
    // public fields.
    if (
        field.visibility == Visibilities.PrivateToThis ||
        field.visibility == Visibilities.Private ||
        field.visibility == Visibilities.Internal
    ) {
        return field
    }

    return null
}
