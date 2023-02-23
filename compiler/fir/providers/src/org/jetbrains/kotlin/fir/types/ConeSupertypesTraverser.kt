/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.types.AbstractSupertypesTraverser
import org.jetbrains.kotlin.types.continueTraversalIf
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class ConeSupertypesTraverser(
    type: SimpleTypeMarker,
    val session: FirSession,
) : AbstractSupertypesTraverser(type, session.newTypeCheckerState()) {
    inline fun <T> lazyTraverseClassSymbols(
        initialValue: T,
        crossinline fold: (T, FirClassSymbol<*>) -> Pair<T, CalculationState>,
    ): DataAccessor<T> {
        return lazyTraverse(initialValue) { old, type ->
            if (type !is ConeLookupTagBasedType) {
                return@lazyTraverse old to CalculationState.CALCULATING
            }

            val symbol = type.lookupTag.toSymbol(session)

            if (symbol !is FirClassSymbol<*>) {
                return@lazyTraverse old to CalculationState.CALCULATING
            }

            fold(old, symbol)
        }
    }

    inline fun anySuperClassSymbol(crossinline condition: (FirClassSymbol<*>) -> Boolean): DataAccessor<Boolean> {
        return lazyTraverseClassSymbols(initialValue = false) { old, symbol ->
            val found = old || condition(symbol)
            found to continueTraversalIf(!found)
        }
    }
}

fun FirSession.newTypeCheckerState() = typeContext.newTypeCheckerState(
    errorTypesEqualToAnything = false,
    stubTypesEqualToAnything = false,
)
