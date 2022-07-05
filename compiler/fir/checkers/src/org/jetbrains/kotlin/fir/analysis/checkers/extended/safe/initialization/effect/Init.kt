/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Errors
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Warm
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

data class Init(override val potential: Warm, val clazz: FirClass, override val symbol: FirBasedSymbol<*>?) : Effect(potential, symbol) {
    override fun Checker.StateOfClass.check(): Errors {                                                     // C-Init
        val effects = potential.effectsOf(this, clazz)
        return effects.flatMap { eff ->
            val eff1 = eff.viewChange(potential)
            eff1.check(this)
        }
        // ???
    }

    override fun createEffectForPotential(pot: Potential) = Init(pot as Warm, clazz, symbol)

    override fun toString(): String {
        return "$potential.init(${clazz.symbol.classId.shortClassName})"
    }
}
