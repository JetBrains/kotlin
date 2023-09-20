/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

private object ExpectForActualAttributeKey : FirDeclarationDataKey()

typealias ExpectForActualMatchingData = Map<ExpectActualMatchingCompatibility<FirBasedSymbol<*>>, List<FirBasedSymbol<*>>>

@SymbolInternals
var FirDeclaration.expectForActual: ExpectForActualMatchingData? by FirDeclarationDataRegistry.data(ExpectForActualAttributeKey)

fun FirFunctionSymbol<*>.getSingleExpectForActualOrNull(): FirFunctionSymbol<*>? =
    (this as FirBasedSymbol<*>).getSingleExpectForActualOrNull() as? FirFunctionSymbol<*>

fun FirBasedSymbol<*>.getSingleExpectForActualOrNull(): FirBasedSymbol<*>? = // todo drop
    expectForActual?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully)?.singleOrNull()

val FirBasedSymbol<*>.expectForActual: ExpectForActualMatchingData?
    get() {
        lazyResolveToPhase(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
        return fir.expectForActual
    }


private object MemberExpectForActualAttributeKey : FirDeclarationDataKey()

// Expect class in the key is needed, because class may correspond to two expects
// in case when two `actual typealias` point to the same class.
typealias MemberExpectForActualData =
        Map<Pair</* actual member */ FirBasedSymbol<*>, /* expect class */ FirRegularClassSymbol>,
                Map</* expect member */ FirBasedSymbol<*>, ExpectActualMatchingCompatibility<*>>>

var FirRegularClass.memberExpectForActual: MemberExpectForActualData? by FirDeclarationDataRegistry.data(MemberExpectForActualAttributeKey)
