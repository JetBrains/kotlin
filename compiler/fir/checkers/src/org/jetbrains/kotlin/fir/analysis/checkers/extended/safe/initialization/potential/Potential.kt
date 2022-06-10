/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.*
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.ClassAnalyser.analyseDeclaration1
import org.jetbrains.kotlin.fir.declarations.*

sealed class Potential(val firElement: FirElement, val length: Int = 0) {
    sealed interface Propagatable {
        fun effectsOf(state: Checker.StateOfClass, firDeclaration: FirDeclaration): Effects =
            state.analyseDeclaration1(firDeclaration).effects

        fun potentialsOf(state: Checker.StateOfClass, firDeclaration: FirDeclaration): Potentials =
            state.analyseDeclaration1(firDeclaration).potentials
    }

    fun Checker.StateOfClass.select(field: FirVariable): EffectsAndPotentials = when {
        field is FirValueParameter -> emptyEffsAndPots
        this@Potential is Root.Cold -> EffectsAndPotentials(Promote(this@Potential))
        length < 4 -> {
            val f = resolveMember(this@Potential, field)
            EffectsAndPotentials(
                FieldAccess(this@Potential, f),
                FieldPotential(this@Potential, f)
            )
        }
        else -> EffectsAndPotentials(Promote(this@Potential))
    }

    fun Checker.StateOfClass.call(function: FirFunction): EffectsAndPotentials {
        val potential = this@Potential
        return when {
            potential is Root.Cold -> EffectsAndPotentials(Promote(potential))
            potential.length < 2 -> {
                val f = resolveMember(potential, function)
                EffectsAndPotentials(
                    MethodAccess(potential, f),
                    MethodPotential(potential, f)
                )
            }
            else -> EffectsAndPotentials(Promote(potential))
        }
    }

    fun outerSelection(clazz: FirClass): EffectsAndPotentials {
        val potential = this@Potential
        return when {
            potential is Root.Cold -> EffectsAndPotentials(Promote(potential))
            potential.length < 2 -> EffectsAndPotentials(OuterPotential(potential, clazz))
            else -> EffectsAndPotentials(Promote(potential))
        }
    }

    abstract fun viewChange(root: Potential): Potential
}