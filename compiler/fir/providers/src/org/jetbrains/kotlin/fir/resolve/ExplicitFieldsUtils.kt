/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.references.FirPropertyWithExplicitBackingFieldResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

private fun FirPropertySymbol.isEffectivelyFinal(session: FirSession): Boolean {
    if (isFinal) return true
    val containingClass = dispatchReceiverType?.toRegularClassSymbol(session)
        ?: return false
    return containingClass.modality == Modality.FINAL && containingClass.classKind != ClassKind.ENUM_CLASS
}

fun FirPropertySymbol.tryAccessExplicitFieldSymbol(
    closestPublicApiInlineFunction: FirFunction?,
    session: FirSession,
    hasVisibleBackingField: Boolean,
): FirBackingFieldSymbol? =
    fir.backingField?.symbol?.takeIf {
        closestPublicApiInlineFunction == null
                && hasVisibleBackingField
                && isEffectivelyFinal(session)
    }

fun FirPropertyWithExplicitBackingFieldResolvedNamedReference.tryAccessExplicitFieldSymbol(
    closestInlineFunction: FirFunction?,
    session: FirSession,
): FirBackingFieldSymbol? =
    (resolvedSymbol as? FirPropertySymbol)?.tryAccessExplicitFieldSymbol(closestInlineFunction, session, hasVisibleBackingField)
