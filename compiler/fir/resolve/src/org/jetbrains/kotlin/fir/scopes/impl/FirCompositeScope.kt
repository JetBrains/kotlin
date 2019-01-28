/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeParameterScope
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.Name

class FirCompositeScope(
    val scopes: MutableList<FirScope>,
    private val reversedPriority: Boolean = false
) : FirScope {
    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeSymbol) -> Boolean
    ): Boolean {
        val scopes = if (reversedPriority) scopes.asReversed() else scopes
        for (scope in scopes) {
            if (!position.allowTypeParameters && scope is FirTypeParameterScope) continue
            if (!scope.processClassifiersByName(name, position, processor)) {
                return false
            }
        }
        return true
    }
}