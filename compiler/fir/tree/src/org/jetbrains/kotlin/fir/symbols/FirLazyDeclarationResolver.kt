/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
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

    inline fun <T> disableLazyResolveContractChecksInside(action: () -> T): T {
        val current = lazyResolveContractChecksEnabled
        lazyResolveContractChecksEnabled = false
        try {
            return action()
        } finally {
            lazyResolveContractChecksEnabled = current
        }
    }

    /**
     * @see org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
     */
    abstract fun lazyResolveToPhase(element: FirElementWithResolveState, toPhase: FirResolvePhase)

    /**
     * @see org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
     */
    abstract fun lazyResolveToPhaseWithCallableMembers(clazz: FirClass, toPhase: FirResolvePhase)

    /**
     * @see org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
     */
    abstract fun lazyResolveToPhaseRecursively(element: FirElementWithResolveState, toPhase: FirResolvePhase)
}

class FirLazyResolveContractViolationException(
    currentPhase: FirResolvePhase,
    requestedPhase: FirResolvePhase,
) : IllegalStateException(
    """
        `lazyResolveToPhase($requestedPhase)` cannot be called from a transformer with a phase $currentPhase.
        `lazyResolveToPhase` can be called only from a transformer with a phase which is strictly greater than a requested phase;
         i.e., `lazyResolveToPhase(A)` may be only called from a lazy transformer with a phase B, where A < B. This is a contract of lazy resolve
     """.trimIndent()
)

val FirSession.lazyDeclarationResolver: FirLazyDeclarationResolver by FirSession.sessionComponentAccessor()

private val FirElementWithResolveState.lazyDeclarationResolver get() = moduleData.session.lazyDeclarationResolver

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
    fir.lazyResolveToPhase(toPhase)
}

/**
 * Lazy resolve [FirElementWithResolveState] to [FirResolvePhase].
 *
 * @see lazyResolveToPhase
 */
fun FirElementWithResolveState.lazyResolveToPhase(toPhase: FirResolvePhase) {
    when (this) {
        is FirSyntheticPropertyAccessor -> delegate.lazyResolveToPhase(toPhase)
        is FirSyntheticProperty -> {
            getter.lazyResolveToPhase(toPhase)
            setter?.lazyResolveToPhase(toPhase)
        }
        else -> lazyDeclarationResolver.lazyResolveToPhase(this, toPhase)
    }
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
    fir.lazyResolveToPhaseWithCallableMembers(toPhase)
}

/**
 * Lazy resolve [FirClass] and its callable members to [FirResolvePhase].
 *
 * @see lazyResolveToPhaseWithCallableMembers
 */
fun FirClass.lazyResolveToPhaseWithCallableMembers(toPhase: FirResolvePhase) {
    lazyDeclarationResolver.lazyResolveToPhaseWithCallableMembers(this, toPhase)
}

/**
 * Lazy resolve [FirBasedSymbol] and all nested declarations to [FirResolvePhase].
 *
 * In the case of lazy resolution (inside Analysis API), it checks that the declaration phase `>= toPhase`.
 * If not, it resolves the declaration for the requested phase.
 *
 * If the [lazyResolveToPhase] is called inside a fir transformer,
 * it should always request the phase which is strictly lower than the current transformer phase,
 * otherwise a deadlock/StackOverflow is possible.
 *
 * For the compiler mode, it does nothing, as the compiler is non-lazy.
 *
 * @receiver [FirBasedSymbol] which should be resolved
 * @param toPhase the minimum phase, the declaration and all nested declarations should be resolved to after an execution of the [lazyResolveToPhase]
 */
fun FirBasedSymbol<*>.lazyResolveToPhaseRecursively(toPhase: FirResolvePhase) {
    fir.lazyResolveToPhaseRecursively(toPhase)
}

/**
 * Lazy resolve [FirElementWithResolveState] and all nested declarations to [FirResolvePhase].
 *
 * @see lazyResolveToPhaseRecursively
 */
fun FirElementWithResolveState.lazyResolveToPhaseRecursively(toPhase: FirResolvePhase) {
    lazyDeclarationResolver.lazyResolveToPhaseRecursively(this, toPhase)
}
