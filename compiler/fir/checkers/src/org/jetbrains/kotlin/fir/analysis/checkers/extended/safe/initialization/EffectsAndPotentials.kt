/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.Effect
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential

data class EffectsAndPotentials(
    val effects: Effects = EmptyEffects,
    val potentials: Potentials = EmptyPotentials
) {
    constructor(effect: Effect, potential: Potential) : this(effect.toEffects(), potential.toPotentials())

    constructor(effect: Effect) : this(effects = effect.toEffects())

    constructor(potential: Potential) : this(potentials = potential.toPotentials())

    operator fun plus(effect: Effect): EffectsAndPotentials = plus(effect.toEffects())

    operator fun plus(potential: Potential): EffectsAndPotentials = plus(potential.toPotentials())

    operator fun plus(effsAndPots: EffectsAndPotentials): EffectsAndPotentials =
        effsAndPots.let { (effs, pots) -> plusEffsAndPots(effs, pots) }

    operator fun plus(effs: Effects): EffectsAndPotentials =
        plusEffsAndPots(effs = effs)

    operator fun plus(pots: Potentials): EffectsAndPotentials =
        plusEffsAndPots(pots = pots)

    private fun plusEffsAndPots(
        effs: Effects = EmptyEffects,
        pots: Potentials = EmptyPotentials
    ): EffectsAndPotentials =
        EffectsAndPotentials(effects + effs, potentials + pots)

    fun maxLength(): Int = potentials.maxOfOrNull(Potential::length) ?: 0

    override fun toString(): String {
        return "($effects, $potentials)"
    }

    companion object {
        val emptyEffsAndPots = EffectsAndPotentials()
    }
}
