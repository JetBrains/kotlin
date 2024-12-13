/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.scopes.FirNoClassifiersScope
import org.jetbrains.kotlin.fir.declarations.FirTowerDataElement
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * Adds filtering to the result of [org.jetbrains.kotlin.fir.declarations.FirTowerDataElement.getAvailableScopes]
 * based on the [position] the context is gathered for.
 */
fun FirTowerDataElement.getAvailableScopesForPosition(
    position: KtElement,
    processTypeScope: FirTypeScope.(ConeKotlinType) -> FirTypeScope = { this },
): List<FirScope> {
    val availableScopes = this.getAvailableScopes(processTypeScope)

    // Filtering out inaccessible classifiers from companion objects, see KT-70108
    return availableScopes.map { scope ->
        val implicitReceiver = implicitReceiver?.boundSymbol
        if ((implicitReceiver as? FirClassSymbol<*>)?.isCompanion == true &&
            implicitReceiver.classId.parentClassId != position.containingClass()?.getClassId() &&
            scope is FirContainingNamesAwareScope
        ) {
            FirNoClassifiersScope(scope)
        } else {
            scope
        }
    }
}