/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.emptyEffsAndPots
import org.jetbrains.kotlin.fir.declarations.FirFunction

data class MethodPotential(override val potential: Potential, val method: FirFunction) : WithPrefix(potential, method) {

    override fun createPotentialForPotential(pot: Potential) = MethodPotential(pot, method)

    override fun propagate(): EffectsAndPotentials {
        return when (potential) {
            is Root.This -> {                                     // P-Inv1
                val state = Checker.resolve(method)
                val potentials = potential.potentialsOf(state, method)
                EffectsAndPotentials(potentials = potentials)
            }
            is Warm -> {                                     // P-Inv2
                val state = Checker.resolve(method)
                val potentials = potential.potentialsOf(state, method)  // find real state
                EffectsAndPotentials(potentials = potentials.viewChange(potential))
            }
            is Root.Cold -> EffectsAndPotentials(potential = potential)
            is Super -> {
                val state = potential.getRightStateOfClass()
                val potentials = potential.potentialsOf(state, method)
                EffectsAndPotentials(potentials = potentials)
            }
            is LambdaPotential -> {
                val (_, pots) = potential.effectsAndPotentials
                EffectsAndPotentials(potentials = pots)
            }
            else -> {                                                       // P-Inv3
                val (effects, potentials) = potential.propagate()
                val call =
                    if (potentials.isNotEmpty()) {
                        val state = Checker.resolve(method)
                        potentials.call(state, method)
                    } else emptyEffsAndPots
                call + effects
            }
        }
    }

    override fun toString(): String {
        return "${this@MethodPotential.potential}.${method.symbol.callableId.callableName}"
    }
}