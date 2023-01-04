/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import java.util.concurrent.CountDownLatch

sealed class FirResolveState {
    abstract val resolvePhase: FirResolvePhase

    abstract override fun toString(): String
}

class FirResolvedToPhaseState private constructor(
    override val resolvePhase: FirResolvePhase
) : FirResolveState() {
    companion object {
        private val phases: List<FirResolvedToPhaseState> = FirResolvePhase.values().map(::FirResolvedToPhaseState)

        operator fun invoke(phase: FirResolvePhase) = phases[phase.ordinal]
    }

    override fun toString(): String = "ResolvedTo($resolvePhase)"
}

fun FirResolvePhase.asResolveState(): FirResolvedToPhaseState = FirResolvedToPhaseState(this)


sealed class FirInProcessOfResolvingToPhaseState : FirResolveState() {
    abstract val resolvingTo: FirResolvePhase
    override val resolvePhase: FirResolvePhase
        get() = FirResolvePhase.values()[resolvingTo.ordinal - 1]

}

class FirInProcessOfResolvingToPhaseStateWithoutLatch private constructor(
    override val resolvingTo: FirResolvePhase
) : FirInProcessOfResolvingToPhaseState() {
    companion object {
        private val phases: List<FirInProcessOfResolvingToPhaseState> =
            FirResolvePhase.values().map(::FirInProcessOfResolvingToPhaseStateWithoutLatch)

        operator fun invoke(phase: FirResolvePhase): FirInProcessOfResolvingToPhaseState {
            require(phase != FirResolvePhase.RAW_FIR) {
                "Cannot resolve to ${FirResolvePhase.RAW_FIR} as it's a first phase"
            }
            return phases[phase.ordinal]
        }
    }

    override fun toString(): String = "ResolvingTo($resolvingTo)"
}

class FirInProcessOfResolvingToPhaseStateWithLatch(
    override val resolvingTo: FirResolvePhase,
    val latch: CountDownLatch,
) : FirInProcessOfResolvingToPhaseState() {
    override fun toString(): String = "ResolvingToWithLatch($resolvePhase)"
}