/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.types.coneType

data class Super(val firSuperReference: FirSuperReference, val firClass: FirClass, override val potential: Potential) :
    WithPrefix(potential, firSuperReference), Potential.Propagatable {
    override fun createPotentialForPotential(pot: Potential) = Super(firSuperReference, firClass, pot)

    override fun propagate(stateOfClass: Checker.StateOfClass): EffectsAndPotentials {
        val (effs, pots) = potential.propagate(stateOfClass)
        return EffectsAndPotentials(effs, pots.wrapPots(::createPotentialForPotential))
    }

    override fun toString(): String {
        return "super@${firSuperReference.superTypeRef.coneType}"
    }
}