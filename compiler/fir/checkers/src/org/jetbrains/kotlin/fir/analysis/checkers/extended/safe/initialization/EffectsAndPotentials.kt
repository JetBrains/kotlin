/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.Effect
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential


val emptyEffsAndPots = EffectsAndPotentials()

data class EffectsAndPotentials(
    val effects: Effects = EmptyEffects,
    val potentials: Potentials = EmptyPotentials
) {
    constructor(effect: Effect, potential: Potential) : this(effect.toEffects(), potential.toPotentials())

    constructor(effect: Effect) : this(effects = effect.toEffects())

    constructor(potential: Potential) : this(potentials = potential.toPotentials())

    operator fun plus(effect: Effect): EffectsAndPotentials = plus(effect.toEffects())

    operator fun plus(potential: Potential): EffectsAndPotentials = plus(potential.toPotentials())

    @JvmName("plus1")
    operator fun plus(effs: Effects): EffectsAndPotentials =
        addEffectsAndPotentials(effs = effs)

    @JvmName("plus2")
    operator fun plus(pots: Potentials): EffectsAndPotentials =
        addEffectsAndPotentials(pots = pots)

    private fun addEffectsAndPotentials(
        effs: Effects = EmptyEffects,
        pots: Potentials = EmptyPotentials
    ): EffectsAndPotentials =
        EffectsAndPotentials(effects + effs, potentials + pots)

    operator fun plus(effectsAndPotentials: EffectsAndPotentials): EffectsAndPotentials =
        effectsAndPotentials.let { (effs, pots) -> addEffectsAndPotentials(effs, pots) }

    fun maxLength(): Int = potentials.maxOfOrNull(Potential::length) ?: 0

    override fun toString(): String {
        return "(Φ=$effects, Π=$potentials)"
    }

    companion object {
    }
}

@JvmName("plus1")
operator fun Effects.plus(effsAndPots: EffectsAndPotentials): EffectsAndPotentials = effsAndPots + this

@JvmName("plus2")
operator fun Potentials.plus(effsAndPots: EffectsAndPotentials): EffectsAndPotentials = effsAndPots + this
