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

typealias ExpectForActualMatchingData = Map<ExpectActualMatchingCompatibility, List<FirBasedSymbol<*>>>

/**
 * Actual declaration -> (many) expect declaration mapping. For top-level declarations.
 *
 * Populated by [FirResolvePhase.EXPECT_ACTUAL_MATCHING] phase for every top-level actual declaration.
 *
 * **Note: Mapping is computed from the declaration-site session of the actual declaration and not refined on the use-sites**
 *
 * See `/docs/fir/k2_kmp.md`
 */
@SymbolInternals
var FirDeclaration.expectForActual: ExpectForActualMatchingData? by FirDeclarationDataRegistry.data(ExpectForActualAttributeKey)

// Used in Compose. It's not clear for how long the compatibility must be preserved.
// Please consult https://jetbrains.team/p/kti/documents/a/18Yt390c5HIq
@Deprecated("Use getSingleMatchedExpectForActualOrNull instead", ReplaceWith("getSingleMatchedExpectForActualOrNull()"))
fun FirFunctionSymbol<*>.getSingleExpectForActualOrNull(): FirFunctionSymbol<*>? =
    getSingleMatchedExpectForActualOrNull()

/**
 * @see expectForActual
 */
fun FirFunctionSymbol<*>.getSingleMatchedExpectForActualOrNull(): FirFunctionSymbol<*>? =
    (this as FirBasedSymbol<*>).getSingleMatchedExpectForActualOrNull() as? FirFunctionSymbol<*>

/**
 * @see expectForActual
 */
fun FirBasedSymbol<*>.getSingleMatchedExpectForActualOrNull(): FirBasedSymbol<*>? =
    expectForActual?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully)?.singleOrNull()

/**
 * @see expectForActual
 */
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
                Map</* expect member */ FirBasedSymbol<*>, ExpectActualMatchingCompatibility>>

/**
 * Actual class + expect class + actual member declaration -> (many) expect member declaration mapping.
 *
 * This attribute allows finding complex actualizations through type-aliases.
 *
 * ```kotlin
 * // MODULE: common
 * expect class A {
 *     fun foo() // expect.1
 * }
 * expect class B {
 *     fun foo() // expect.2
 * }
 *
 * // MODULE: platform()(common)
 *
 * actual typealias A = Impl
 * actual typealias B = Impl
 *
 * // attribute: memberExpectForActual
 * // value: {
 * //  (symbol: expect class A, symbol: impl.1) => { symbol: expect.1 => Compatible },
 * //  (symbol: expect class B, symbol: impl.1) => { symbol: expect.2 => Compatible },
 * // }
 * class Impl {
 *     fun foo() {} // impl.1
 * }
 * ```
 *
 * Populated by [FirResolvePhase.EXPECT_ACTUAL_MATCHING] phase for every top-level class declaration that is either actual class or
 * expansion of actual type-alias.
 *
 * **Note: Mapping is computed from the declaration-site session of the actual declaration and not refined on the use-sites**
 *
 * See `/docs/fir/k2_kmp.md`
 */
// TODO this cache is questionable. Maybe we want to drop it KT-62913
var FirRegularClass.memberExpectForActual: MemberExpectForActualData? by FirDeclarationDataRegistry.data(MemberExpectForActualAttributeKey)
