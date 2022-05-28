/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization


val emptyEffsAndPots = EffectsAndPotentials()

data class EffectsAndPotentials(
    val effects: Effects = emptyList(),
    val potentials: Potentials = emptyList()
) {
    constructor(effect: Effect, potential: Potential) : this(listOf(effect), listOf(potential))

    constructor(effect: Effect) : this(effects = listOf(effect))

    constructor(potential: Potential) : this(potentials = listOf(potential))

    operator fun plus(effect: Effect): EffectsAndPotentials = plus(listOf(effect))

    operator fun plus(potential: Potential): EffectsAndPotentials = plus(listOf(potential))

    @JvmName("plus1")
    operator fun plus(effs: Effects): EffectsAndPotentials =
        addEffectsAndPotentials(effs = effs)

    @JvmName("plus2")
    operator fun plus(pots: Potentials): EffectsAndPotentials =
        addEffectsAndPotentials(pots = pots)

    fun addEffectsAndPotentials(
        effs: Effects = emptyList(),
        pots: Potentials = emptyList()
    ): EffectsAndPotentials =
        EffectsAndPotentials(effects + effs, potentials + pots)

    operator fun plus(effectsAndPotentials: EffectsAndPotentials): EffectsAndPotentials =
        effectsAndPotentials.let { (effs, pots) -> addEffectsAndPotentials(effs, pots) }

    fun maxLength(): Int = potentials.maxOfOrNull(Potential::length) ?: 0

    companion object {
        fun Potentials.toEffectsAndPotentials() = EffectsAndPotentials(potentials = this)
    }
}

@JvmName("plus1")
operator fun Effects.plus(effsAndPots: EffectsAndPotentials): EffectsAndPotentials = effsAndPots + this
@JvmName("plus2")
operator fun Potentials.plus(effsAndPots: EffectsAndPotentials): EffectsAndPotentials = effsAndPots + this
