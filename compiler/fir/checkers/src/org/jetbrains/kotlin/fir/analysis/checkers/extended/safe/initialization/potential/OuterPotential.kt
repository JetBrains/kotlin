/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Promote
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.emptyEffsAndPots
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.outerSelection
import org.jetbrains.kotlin.fir.declarations.FirClass

data class OuterPotential(override val potential: Potential, val outerClass: FirClass) :
    WithPrefix(potential, outerClass) {

    override fun createPotentialForPotential(pot: Potential) = OuterPotential(pot, outerClass)

    override fun propagate(): EffectsAndPotentials {
        return when (potential) {
            is Root.This -> emptyEffsAndPots                // P-Out1
            is Warm -> {                               // P-Out2
                val (_, outerPot) = potential
                return EffectsAndPotentials(outerPot)
                // TODO:
                //  if (firClass != this.firClass) rec: findParent(firClass)
                //  просто вверх по цепочке наследования если inner от кого-то наследуется
            }
            is Root.Cold -> EffectsAndPotentials(Promote(potential))  // or exception or empty list
            is Super -> TODO()
            is FunPotential -> throw IllegalArgumentException()
            else -> {                                                       // P-Out3
                val (effects, potentials) = potential.propagate()
                val (_, outPots) = outerSelection(potentials, outerClass)
                EffectsAndPotentials(effects, outPots)
            }
        }
    }

    override fun toString(): String {
        return "$potential.outer(${outerClass.symbol.classId}))"
    }
}
