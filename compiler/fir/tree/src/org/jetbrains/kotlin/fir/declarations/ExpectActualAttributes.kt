/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import java.util.*

private object ExpectForActualAttributeKey : FirDeclarationDataKey()
private object ActualForExpectAttributeKey : FirDeclarationDataKey()

typealias ExpectForActualData = Map<ExpectActualCompatibility<FirBasedSymbol<*>>, List<FirBasedSymbol<*>>>

@SymbolInternals
var FirDeclaration.expectForActual: ExpectForActualData? by FirDeclarationDataRegistry.data(ExpectForActualAttributeKey)
private var FirDeclaration.actualForExpectMap: WeakHashMap<FirSession, FirBasedSymbol<*>>? by FirDeclarationDataRegistry.data(ActualForExpectAttributeKey)

val FirBasedSymbol<*>.expectForActual: ExpectForActualData?
    get() {
        ensureResolved(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
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
    ensureResolved(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
    return fir.getActualForExpect(session)
}

@SymbolInternals
fun FirDeclaration.setActualForExpect(useSiteSession: FirSession, actualSymbol: FirBasedSymbol<*>) {
    val map = getOrCreateActualForExpectMap()
    map[useSiteSession] = actualSymbol
}

