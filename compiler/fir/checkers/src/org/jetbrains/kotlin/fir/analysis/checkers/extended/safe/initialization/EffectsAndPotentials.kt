/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

data class EffectsAndPotentials(
    val effects: List<Effect> = listOf(),
    val potentials: List<Potential> = listOf()
) {
    constructor(effect: Effect, potential: Potential) : this(listOf(effect), listOf(potential))

    constructor(effect: Effect) : this(effects = listOf(effect))

    constructor(potential: Potential) : this(potentials = listOf(potential))

    operator fun plus(effect: Effect): EffectsAndPotentials = plus(listOf(effect))

    operator fun plus(potential: Potential): EffectsAndPotentials = plus(listOf(potential))

    @JvmName("plus1")
    operator fun plus(effs: List<Effect>): EffectsAndPotentials =
        addEffectsAndPotentials(effs = effs)

    @JvmName("plus2")
    operator fun plus(pots: List<Potential>): EffectsAndPotentials =
        addEffectsAndPotentials(pots = pots)

    fun addEffectsAndPotentials(
        effs: List<Effect> = listOf(),
        pots: List<Potential> = listOf()
    ): EffectsAndPotentials =
        EffectsAndPotentials(effects + effs, potentials + pots)

    operator fun plus(effectsAndPotentials: EffectsAndPotentials): EffectsAndPotentials =
        effectsAndPotentials.let { (effs, pots) -> addEffectsAndPotentials(effs, pots) }

    fun maxLength(): Int = potentials.maxOfOrNull(Potential::length) ?: 0
}