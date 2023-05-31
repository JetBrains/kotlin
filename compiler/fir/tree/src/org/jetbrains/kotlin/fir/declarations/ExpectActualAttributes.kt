/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.compatible

private object ExpectForActualAttributeKey : FirDeclarationDataKey()

typealias ExpectForActualData = Map<ExpectActualCompatibility<FirBasedSymbol<*>>, List<FirBasedSymbol<*>>>

@SymbolInternals
var FirDeclaration.expectForActual: ExpectForActualData? by FirDeclarationDataRegistry.data(ExpectForActualAttributeKey)

fun FirFunctionSymbol<*>.getSingleExpectForActualOrNull(compatibleOnly: Boolean = false) =
    (this as FirBasedSymbol<*>).getSingleExpectForActualOrNull(compatibleOnly) as? FirFunctionSymbol<*>

fun FirBasedSymbol<*>.getSingleExpectForActualOrNull(compatibleOnly: Boolean = false): FirBasedSymbol<*>? {
    val expectForActual = expectForActual ?: return null
    var actuals: List<FirBasedSymbol<*>>? = null
    for ((key, item) in expectForActual) {
        if (!compatibleOnly || key.compatible) {
            if (actuals == null) {
                actuals = item
            } else {
                return null // Exit if there are more than one list with actuals
            }
        }
    }
    return actuals?.singleOrNull()
}

val FirBasedSymbol<*>.expectForActual: ExpectForActualData?
    get() {
        lazyResolveToPhase(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
        return fir.expectForActual
    }

