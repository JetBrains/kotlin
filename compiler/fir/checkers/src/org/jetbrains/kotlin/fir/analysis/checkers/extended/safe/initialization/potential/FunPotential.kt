/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction

data class FunPotential(
    val effectsAndPotentials: EffectsAndPotentials,
    val anonymousFunction: FirAnonymousFunction
) : Potential(anonymousFunction, effectsAndPotentials.maxLength()) {

    override fun viewChange(root: Potential): Potential {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "Fun($effectsAndPotentials, ${anonymousFunction.symbol.callableId.callableName})"
    }
}