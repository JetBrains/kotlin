/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Error
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Errors
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.*
import org.jetbrains.kotlin.fir.declarations.FirFunction

data class MethodAccess(override val potential: Potential, var method: FirFunction) : Effect(potential, method.symbol) {
    override fun Checker.StateOfClass.check(): Errors {
        return when (potential) {
            is Root.This -> {                                     // C-Inv1
                val state = Checker.resolve(method)
                potential.effectsOf(state, method).flatMap { it.check(this) }
            }
            is Warm -> {                                     // C-Inv2
                val state = Checker.resolve(method)
                potential.effectsOf(state, method).flatMap { eff ->
                    eff.viewChange(potential)
                    eff.check(this)
                }
            }
            is LambdaPotential -> {                                 // invoke
                potential.effsAndPots.effects.flatMap { it.check(this) }
            }
            is Root.Cold -> listOf(Error.InvokeError(this@MethodAccess))              // illegal
            is Super -> {
                val state = potential.getRightStateOfClass()
                potential.effectsOf(state, method).flatMap { it.check(this) }
            }
            else ->                                                         // C-Inv3
                ruleAcc3(potential.propagate())
        }
    }

    override fun createEffectForPotential(pot: Potential) = MethodAccess(pot, method)

    override fun toString(): String {
        return "$potential.${method.symbol.callableId.callableName}◊"
    }
}