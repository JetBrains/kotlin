/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

/**
 * A component to lazy resolve [FirBasedSymbol] to the required phase.
 *
 * This is needed for the Analysis API to work properly, for the compiler the implementation does nothing.
 *
 * @see org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
 */
abstract class FirLazyDeclarationResolver : FirSessionComponent {
    var lazyResolveContractChecksEnabled: Boolean = true

    abstract fun startResolvingPhase(phase: FirResolvePhase)

    abstract fun finishResolvingPhase(phase: FirResolvePhase)

    fun disableLazyResolveContractChecks() {
        lazyResolveContractChecksEnabled = false
    }

    inline fun disableLazyResolveContractChecksInside(action: () -> Unit) {
        val current = lazyResolveContractChecksEnabled
        lazyResolveContractChecksEnabled = false
        try {
            action()
        } finally {
            lazyResolveContractChecksEnabled = current
        }
    }

    abstract fun lazyResolveToPhase(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase)
    abstract fun lazyResolveToPhaseWithCallableMembers(symbol: FirClassSymbol<*>, toPhase: FirResolvePhase)
}

val FirSession.lazyDeclarationResolver: FirLazyDeclarationResolver by FirSession.sessionComponentAccessor()

private val FirDeclaration.lazyDeclarationResolver get() = moduleData.session.lazyDeclarationResolver

/**
 * Lazy resolve [FirBasedSymbol] to [FirResolvePhase].
 *
 * In the case of lazy resolution (inside Analysis API), it checks that the declaration phase `>= toPhase`.
 * If not, it resolves the declaration for the requested phase.
 *
 * If the [lazyResolveToPhase] is called inside a fir transformer,
 * it should always request the phase which is strictly lower than the current transformer phase, otherwise a deadlock/StackOverflow is possible.
 *
 * For the compiler mode, it does nothing, as the compiler is non-lazy.
 *
 * @receiver [FirBasedSymbol] which should be resolved
 * @param toPhase the minimum phase, the declaration should be resolved to after an execution of the [lazyResolveToPhase]
 */
fun FirBasedSymbol<*>.lazyResolveToPhase(toPhase: FirResolvePhase) {
    fir.lazyDeclarationResolver.lazyResolveToPhase(this, toPhase)
}

/**
 * Lazy resolve [FirDeclaration] to [FirResolvePhase].
 *
 * @see lazyResolveToPhase
 */
fun FirDeclaration.lazyResolveToPhase(toPhase: FirResolvePhase) {
    symbol.lazyResolveToPhase(toPhase)
}

/**
 * Lazy resolve [FirClassSymbol] and its callable members to [FirResolvePhase].
 *
 * Might resolve additional required declarations.
 *
 * @receiver [FirClassSymbol] which should be resolved and which callable members should be resolved
 * @param toPhase the minimum phase, the declaration and callable members should be resolved
 * to after an execution of the [lazyResolveToPhaseWithCallableMembers]
 *
 * Can be used instead of [lazyResolveToPhase] to avoid extra resolve calls.
 * Effectively the same as:
 * ```
 * kclass.lazyResolveToPhase(phase)
 * kclass.callableDeclarations.forEach { it.lazyResolveToPhase(phase) }
 * ```
 *
 * @see lazyResolveToPhase
 */

fun FirClassSymbol<*>.lazyResolveToPhaseWithCallableMembers(toPhase: FirResolvePhase) {
    fir.lazyDeclarationResolver.lazyResolveToPhaseWithCallableMembers(this, toPhase)
}

/**
 * Lazy resolve [FirClass] and its callable members to [FirResolvePhase].
 *
 * @see lazyResolveToPhaseWithCallableMembers
 */
fun FirClass.lazyResolveToPhaseWithCallableMembers(toPhase: FirResolvePhase) {
    symbol.lazyResolveToPhaseWithCallableMembers(toPhase)
}
