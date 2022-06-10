/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.FirElement

sealed class WithPrefix(open val potential: Potential, firElement: FirElement) : Potential(firElement, potential.length + 1) {

    // TODO: Normal name
    abstract fun createPotentialForPotential(pot: Potential): Potential

    override fun viewChange(root: Potential): Potential {
        val viewedPot = potential.viewChange(root)
        return createPotentialForPotential(viewedPot)
    }
}