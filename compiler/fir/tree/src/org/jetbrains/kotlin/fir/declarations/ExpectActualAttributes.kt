/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.compatible
import java.util.*

private object ExpectForActualAttributeKey : FirDeclarationDataKey()
private object ActualForExpectAttributeKey : FirDeclarationDataKey()

typealias ExpectForActualData = Map<ExpectActualCompatibility<FirBasedSymbol<*>>, List<FirBasedSymbol<*>>>

@SymbolInternals
var FirDeclaration.expectForActual: ExpectForActualData? by FirDeclarationDataRegistry.data(ExpectForActualAttributeKey)
private var FirDeclaration.actualForExpectMap: WeakHashMap<FirSession, FirBasedSymbol<*>>? by FirDeclarationDataRegistry.data(ActualForExpectAttributeKey)

fun FirFunctionSymbol<*>.getSingleCompatibleExpectForActualOrNull() =
    (this as FirBasedSymbol<*>).getSingleCompatibleExpectForActualOrNull() as? FirFunctionSymbol<*>

fun FirBasedSymbol<*>.getSingleCompatibleExpectForActualOrNull(): FirBasedSymbol<*>? {
    val expectForActual = expectForActual ?: return null
    var compatibleActuals: List<FirBasedSymbol<*>>? = null
    for ((key, item) in expectForActual) {
        if (key.compatible) {
            if (compatibleActuals == null) {
                compatibleActuals = item
            } else {
                return null // Exit if there are more than one list with compatible actuals
            }
        }
    }
    return compatibleActuals?.singleOrNull()
}

val FirBasedSymbol<*>.expectForActual: ExpectForActualData?
    get() {
        lazyResolveToPhase(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
        return fir.expectForActual
    }

private fun FirDeclaration.getOrCreateActualForExpectMap(): WeakHashMap<FirSession, FirBasedSymbol<*>> {
    var map = actualForExpectMap
    if (map != null) return map
    synchronized(this) {
        map = actualForExpectMap
        if (map == null) {
            map = WeakHashMap()
            actualForExpectMap = map
        }
    }
    return map!!
}

@SymbolInternals
fun FirDeclaration.getActualForExpect(useSiteSession: FirSession): FirBasedSymbol<*>? {
    return actualForExpectMap?.get(useSiteSession)
}

fun FirBasedSymbol<*>.getActualForExpect(session: FirSession): FirBasedSymbol<*>? {
    lazyResolveToPhase(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
    return fir.getActualForExpect(session)
}

@SymbolInternals
fun FirDeclaration.setActualForExpect(useSiteSession: FirSession, actualSymbol: FirBasedSymbol<*>) {
    val map = getOrCreateActualForExpectMap()
    map[useSiteSession] = actualSymbol
}

