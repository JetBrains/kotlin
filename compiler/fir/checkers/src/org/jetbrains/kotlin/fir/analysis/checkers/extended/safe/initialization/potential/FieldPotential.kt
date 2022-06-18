/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.*
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials.Companion.toEffectsAndPotentials
import org.jetbrains.kotlin.fir.declarations.FirVariable

data class FieldPotential(override val potential: Potential, val field: FirVariable) : WithPrefix(potential, field) {

    override fun createPotentialForPotential(pot: Potential) = FieldPotential(pot, field)

    override fun propagate(): EffectsAndPotentials {
        return when (potential) {
            is Root.This -> {                                  // P-Acc1
                val state = Checker.resolve(field)
                val potentials = potential.potentialsOf(state, field)
                potentials.viewChange(potential).toEffectsAndPotentials()
            }
            is Warm -> {                                         // P-Acc2
                val state = Checker.resolve(field)
                val potentials = potential.potentialsOf(state, field)
                potentials.viewChange(potential).toEffectsAndPotentials()
            }
            is Root.Cold -> EffectsAndPotentials(potential) // or exception or empty list
            is Super -> {
                val state = potential.getRightStateOfClass()
                val potentials = potential.potentialsOf(state, field)
                potentials.viewChange(potential).toEffectsAndPotentials()
            }
            is LambdaPotential -> throw IllegalArgumentException()
            else -> {                                                       // P-Acc3
                val (effects, potentials) = potential.propagate()
                val select = if (potentials.isNotEmpty()) {
                    val state = Checker.resolve(field)
                    state.select(potentials, field)
                } else emptyEffsAndPots

                select + effects
            }
        }
    }

    override fun toString(): String {
        return "$potential.${field.symbol.callableId.callableName}"
    }
}