/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.initialization

class EffectsAndPotentials(
    val effects: List<Effect> = listOf(),
    val potentials: List<Potential> = listOf()
) {

    constructor(effect: Effect, potential: Potential) : this(listOf(effect), listOf(potential))

    constructor(effect: Effect) : this(effects = listOf(effect))

    constructor(potential: Potential) : this(potentials = listOf(potential))

    fun addEffectAndPotential(effect: Effect, potential: Potential): EffectsAndPotentials =
        addEffectsAndPotentials(listOf(effect), listOf(potential))

    fun addEffectsAndPotentials(
        effs: List<Effect> = listOf(),
        pots: List<Potential> = listOf()
    ): EffectsAndPotentials =
        EffectsAndPotentials(effects + effs, potentials + pots)

    fun addEffectsAndPotentials(effectsAndPotentials: EffectsAndPotentials): EffectsAndPotentials =
        addEffectsAndPotentials(effectsAndPotentials.effects, effectsAndPotentials.potentials)
}