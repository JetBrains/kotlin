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
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.dfa.DfaInternals
import org.jetbrains.kotlin.fir.resolve.dfa.symbol

@OptIn(DfaInternals::class)
data class Promote(override val potential: Potential) : Effect(potential, potential.firElement.symbol) {
    override fun StateOfClass.check(): Errors = // C-Up2
        when (potential) {
            is Warm -> {                                     // C-Up1
                potential.clazz.declarations.map {
                    when (it) {
//                                is FirAnonymousInitializer -> TODO()
                        is FirRegularClass -> TODO()
//                                is FirConstructor -> TODO()
                        is FirSimpleFunction -> TODO()
                        is FirField, is FirProperty -> TODO()
                        else -> throw IllegalArgumentException()
                    }
                }
            }
            is LambdaPotential -> ruleAcc3(potential.effsAndPots)
            is Root -> listOf(Error.PromoteError(this@Promote))
            else -> ruleAcc3(potential.propagate(this))
        }

    override fun createEffectForPotential(pot: Potential) = Promote(pot)

    override fun toString(): String {
        return "$potential↑"
    }
}