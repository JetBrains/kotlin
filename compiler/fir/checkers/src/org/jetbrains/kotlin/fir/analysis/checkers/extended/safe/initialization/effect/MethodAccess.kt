/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.StateOfClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Error
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Errors
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.LambdaPotential
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Root
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Warm
import org.jetbrains.kotlin.fir.declarations.FirFunction

data class MethodAccess(override val potential: Potential, var method: FirFunction) : Effect(potential, method.symbol) {
    override fun StateOfClass.check(): Errors =
        when (potential) {
            is Warm -> potential.effectsOf(this, method).flatMap { eff ->
                eff.viewChange(potential)
                eff.check(this)
            }
            is LambdaPotential -> potential.effsAndPots.effects.flatMap { it.check(this) }
            is Root.Cold -> listOf(Error.InvokeError(this@MethodAccess))              // illegal
            is Potential.Propagatable -> potential.effectsOf(this, method).flatMap { it.check(this) }
            else ->                                                         // C-Inv3
                ruleAcc3(potential.propagate(this))
        }

    override fun createEffectForPotential(pot: Potential) = MethodAccess(pot, method)

    override fun toString(): String {
        return "$potential.${method.symbol.callableId.callableName}◊"
    }
}