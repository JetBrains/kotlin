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
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.isCompatibleOrWeaklyIncompatible

private object ExpectForActualAttributeKey : FirDeclarationDataKey()

typealias ExpectForActualData = Map<ExpectActualCompatibility<FirBasedSymbol<*>>, List<FirBasedSymbol<*>>>

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
var FirDeclaration.expectForActual: ExpectForActualData? by FirDeclarationDataRegistry.data(ExpectForActualAttributeKey)

/**
 * @see expectForActual
 */
fun FirFunctionSymbol<*>.getSingleExpectForActualOrNull(): FirFunctionSymbol<*>? =
    (this as FirBasedSymbol<*>).getSingleExpectForActualOrNull() as? FirFunctionSymbol<*>

/**
 * @see expectForActual
 */
fun FirBasedSymbol<*>.getSingleExpectForActualOrNull(): FirBasedSymbol<*>? {
    val expectForActual = expectForActual ?: return null
    val compatibleOrWeakCompatible: List<FirBasedSymbol<*>> =
        expectForActual.entries.singleOrNull { it.key.isCompatibleOrWeaklyIncompatible }?.value ?: return null
    return compatibleOrWeakCompatible.singleOrNull()
}

/**
 * @see expectForActual
 */
val FirBasedSymbol<*>.expectForActual: ExpectForActualData?
    get() {
        lazyResolveToPhase(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
        return fir.expectForActual
    }


private object MemberExpectForActualAttributeKey : FirDeclarationDataKey()

// Expect class in the key is needed, because class may correspond to two expects
// in case when two `actual typealias` point to the same class.
typealias MemberExpectForActualData =
        Map<Pair</* actual member */ FirBasedSymbol<*>, /* expect class */ FirRegularClassSymbol>,
                Map</* expect member */ FirBasedSymbol<*>, ExpectActualCompatibility<*>>>

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
var FirRegularClass.memberExpectForActual: MemberExpectForActualData? by FirDeclarationDataRegistry.data(MemberExpectForActualAttributeKey)
