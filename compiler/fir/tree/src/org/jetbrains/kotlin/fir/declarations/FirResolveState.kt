/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

interface FirResolveState {
    val resolvePhase: FirResolvePhase

    override fun toString(): String
}

class FirResolvedToPhaseState private constructor(
    override val resolvePhase: FirResolvePhase
) : FirResolveState {
    companion object {
        private val phases: List<FirResolvedToPhaseState> = FirResolvePhase.values().map(::FirResolvedToPhaseState)

        operator fun invoke(phase: FirResolvePhase) = phases[phase.ordinal]
    }

    override fun toString(): String = "ResolvedTo($resolvePhase)"
}

fun FirResolvePhase.asResolveState(): FirResolvedToPhaseState = FirResolvedToPhaseState(this)