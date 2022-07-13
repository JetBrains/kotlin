/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials
import org.jetbrains.kotlin.fir.declarations.FirClass

data class Warm(val clazz: FirClass, override val potential: Potential) : WithPrefix(potential, clazz), Potential.Propagatable {

    override fun viewChange(root: Potential) = when {
        potential is Root.Cold -> this
        // The original solution used the replacement of such as potential.viewChange(Cold(clazz)) if the nesting was too large
        // This is roughly equivalent to what is written
        potential.length > 2 -> createPotentialForPotential(Root.Cold(clazz))
        else -> super.viewChange(root)
    }

    override fun propagate(stateOfClass: Checker.StateOfClass) = EffectsAndPotentials(this)

    override fun createPotentialForPotential(pot: Potential) = Warm(clazz, pot)

    override fun toString(): String {
        return "warm(${clazz.symbol.toLookupTag()}, $potential)"
    }
}