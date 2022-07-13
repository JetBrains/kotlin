/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.StateOfClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Effects
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Errors
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

sealed class Effect(open val potential: Potential, open val symbol: FirBasedSymbol<*>?) {
    protected abstract fun StateOfClass.check(): Errors

    protected abstract fun createEffectForPotential(pot: Potential): Effect

    @JvmName("check1")
    fun check(stateOfClass: StateOfClass): Errors = stateOfClass.run {
        if (this@Effect in effectsInProcess) return emptyList()
        effectsInProcess.add(this@Effect)

        val errors = check()

        for (error in errors) error.addEffectToTrace(this@Effect)

        effectsInProcess.remove(this@Effect)
        return errors
    }

    protected fun StateOfClass.ruleAcc3(
        effsAndPots: EffectsAndPotentials
    ): Errors =
        effsAndPots.run {
            val errors = potentials.map { createEffectForPotential(it).check(this@ruleAcc3) } // call / select
            val effectErrors = effects.map { it.check(this@ruleAcc3) }
            (errors + effectErrors).flatten()
        }

    fun viewChange(root: Potential): Effect {
        val viewedPot = potential.viewChange(root)
        return createEffectForPotential(viewedPot)
    }

    fun toEffects() = Effects(this)
}