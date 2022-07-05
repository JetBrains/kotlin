/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Error
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Errors
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.*
import org.jetbrains.kotlin.fir.declarations.FirVariable

data class FieldAccess(override val potential: Potential, val field: FirVariable) : Effect(potential, field.symbol) {
    override fun Checker.StateOfClass.check(): Errors {
        return when (potential) {
            is Root.This, is Super -> {                                     // C-Acc1
                if (field.isPropertyInitialized()) emptyList()
                else listOf(Error.AccessError(this@FieldAccess))
            }
            is Warm -> emptyList()                              // C-Acc2
            is Root.Cold -> listOf(Error.AccessError(this@FieldAccess))           // illegal
            is LambdaPotential -> throw IllegalArgumentException()                  // impossible
            else ->                                                         // C-Acc3
                ruleAcc3(potential.propagate())
        }
    }

    override fun createEffectForPotential(pot: Potential) = FieldAccess(pot, field)

    override fun toString(): String {
        return "$potential.${field.symbol.callableId.callableName}!"
    }
}