/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.canNarrowDownGetterType
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.references.FirPropertyWithExplicitBackingFieldResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

private fun FirPropertySymbol.isEffectivelyFinal(session: FirSession): Boolean {
    if (isFinal) return true
    val containingClass = dispatchReceiverType?.toRegularClassSymbol(session)
        ?: return false
    return containingClass.modality == Modality.FINAL && containingClass.classKind != ClassKind.ENUM_CLASS
}

fun FirPropertyWithExplicitBackingFieldResolvedNamedReference.tryAccessExplicitFieldSymbol(
    closestInlineFunction: FirFunction?,
    session: FirSession,
): FirBasedSymbol<*> {
    val propertyReceiver = resolvedSymbol as? FirPropertySymbol ?: return resolvedSymbol
    val visibilityCheckResult = closestInlineFunction?.visibility?.compareTo(Visibilities.Private)

    if (visibilityCheckResult != null && visibilityCheckResult > 0) {
        return resolvedSymbol
    }

    if (
        propertyReceiver.isEffectivelyFinal(session) &&
        hasVisibleBackingField &&
        propertyReceiver.canNarrowDownGetterType
    ) {
        return propertyReceiver.fir.backingField?.symbol ?: resolvedSymbol
    }

    return resolvedSymbol
}
