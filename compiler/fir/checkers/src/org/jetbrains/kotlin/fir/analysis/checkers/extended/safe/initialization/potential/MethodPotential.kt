/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials.Companion.emptyEffsAndPots
import org.jetbrains.kotlin.fir.declarations.FirFunction

data class MethodPotential(override val potential: Potential, val method: FirFunction) : WithPrefix(potential, method) {

    override fun createPotentialForPotential(pot: Potential) = MethodPotential(pot, method)

    override fun propagate(): EffectsAndPotentials {
        return when (potential) {
            is Root.This -> {                                     // P-Inv1
                val potentials = potential.potentialsOf(stateOfClass, method)
                EffectsAndPotentials(potentials = potentials)
            }
            is Warm -> {                                     // P-Inv2
                val potentials = potential.potentialsOf(stateOfClass, method)  // find real state
                EffectsAndPotentials(potentials = potentials.viewChange(potential))
            }
            is Root.Cold -> EffectsAndPotentials(potential = potential)
            is Super -> {
                val potentials = potential.potentialsOf(stateOfClass, method)
                EffectsAndPotentials(potentials = potentials)
            }
            is LambdaPotential -> {
                val (_, pots) = potential.effsAndPots
                EffectsAndPotentials(potentials = pots)
            }
            else -> {                                                       // P-Inv3
                val (effects, potentials) = potential.propagate(stateOfClass)
                val call = potentials.call(stateOfClass, method)
                call + effects
            }
        }
    }

    override fun toString(): String {
        return "${this@MethodPotential.potential}.${method.symbol.callableId.callableName}"
    }
}