/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.StateOfClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Effects
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potentials
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.FieldAccess
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.MethodAccess
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.Promote
import org.jetbrains.kotlin.fir.declarations.*

sealed class Potential(val firElement: FirElement, val length: Int = 0) {
    sealed interface Propagatable {
        fun effectsOf(stateOfClass: StateOfClass, firClass: FirClass): Effects =
            stateOfClass.analyseDeclaration(firClass).effects

        fun effectsOf(stateOfClass: StateOfClass, firDeclaration: FirCallableDeclaration): Effects {
            val state = stateOfClass.resolve(firDeclaration)
            return state.analyseDeclaration(firDeclaration).effects
        }

        fun potentialsOf(stateOfClass: StateOfClass, firDeclaration: FirCallableDeclaration): Potentials {
            val state = stateOfClass.resolve(firDeclaration)
            return state.analyseDeclaration(firDeclaration).potentials
        }

        private fun StateOfClass.analyseDeclaration(firDeclaration: FirDeclaration): EffectsAndPotentials =
            caches.getOrPut(firDeclaration) { firDeclaration.accept(declarationVisitor, null) }
    }

    abstract fun propagate(stateOfClass: StateOfClass): EffectsAndPotentials

    abstract fun viewChange(root: Potential): Potential

    fun select(stateOfClass: StateOfClass, field: FirVariable) =
        when {
            this is Root.Cold -> EffectsAndPotentials(Promote(this))
            length < 4 -> {
                val f = stateOfClass.resolveMember(this, field)
                EffectsAndPotentials(
                    FieldAccess(this, f),
                    FieldPotential(this, f)
                )
            }
            else -> EffectsAndPotentials(Promote(this))
        }

    fun call(stateOfClass: StateOfClass, function: FirFunction): EffectsAndPotentials {
        val potential = this
        return when {
            potential is Root.Cold -> EffectsAndPotentials(Promote(potential))
            potential.length < 2 -> {
                val f = stateOfClass.resolveMember(function)
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

    fun toPotentials() = Potentials(this)
}