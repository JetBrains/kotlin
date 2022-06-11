/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.viewChange
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction

data class LambdaPotential(
    val effectsAndPotentials: EffectsAndPotentials,
    val anonymousFunction: FirAnonymousFunction
) : Potential(anonymousFunction, effectsAndPotentials.maxLength()) {

    override fun propagate() = EffectsAndPotentials(this)

    override fun viewChange(root: Potential): Potential {
        val (effs, pots) = effectsAndPotentials
        val viewedEffsAndPots = EffectsAndPotentials(effs.viewChange(root), pots.viewChange(root))
        return LambdaPotential(viewedEffsAndPots, anonymousFunction)
    }

    override fun toString(): String {
        return "Fun($effectsAndPotentials, ${anonymousFunction.symbol.callableId.callableName})"
    }
}