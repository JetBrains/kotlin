/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.FunPotential
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Root
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Warm
import org.jetbrains.kotlin.fir.declarations.*

typealias Effects = List<Effect>

data class FieldAccess(override val potential: Potential, val field: FirVariable) : Effect(potential) {
    override fun Checker.StateOfClass.check(): Errors {
        return when (potential) {
            is Root.This, is Root.Super -> {                                     // C-Acc1
                if (field.isPropertyInitialized())
                    emptyList()
                else listOf(Error.AccessError(this@FieldAccess))
            }
            is Warm -> emptyList()                              // C-Acc2
            is Root.Cold -> listOf(Error.AccessError(this@FieldAccess))           // illegal
            is FunPotential -> throw Exception()                  // impossible
            else ->                                                         // C-Acc3
                ruleAcc3(potential.propagate())
        }
    }

    override fun createEffectForPotential(pot: Potential) = FieldAccess(pot, field)

    override fun toString(): String {
        return "$potential.${field.symbol.callableId.callableName}!"
    }
}

data class MethodAccess(override val potential: Potential, var method: FirFunction) : Effect(potential) {
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
            is FunPotential -> emptyList() // invoke
            is Root.Cold -> listOf(Error.InvokeError(this@MethodAccess))              // illegal
            is Root.Super -> {
                val state = Checker.cache[potential.firClass] ?: TODO()
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

data class Promote(override val potential: Potential) : Effect(potential) {
    override fun Checker.StateOfClass.check(): Errors = // C-Up2
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
            is FunPotential -> ruleAcc3(potential.effectsAndPotentials)
            is Root -> listOf(Error.PromoteError(this@Promote))
            else -> ruleAcc3(potential.propagate())
        }

    override fun createEffectForPotential(pot: Potential) = Promote(pot)

    override fun toString(): String {
        return "$potential↑"
    }
}

data class Init(override val potential: Warm, val clazz: FirClass) : Effect(potential) {
    override fun Checker.StateOfClass.check(): Errors {                                                     // C-Init
        val effects = potential.effectsOf(this, clazz)
        return effects.flatMap { eff ->
            val eff1 = eff.viewChange(potential)
            eff1.check(this)
        }
        // ???
    }

    override fun createEffectForPotential(pot: Potential) = Init(pot as Warm, clazz)

    override fun toString(): String {
        return "$potential.init(${clazz.symbol.classId.shortClassName})"
    }
}

sealed class Effect(open val potential: Potential) {
    protected abstract fun Checker.StateOfClass.check(): Errors

    protected abstract fun createEffectForPotential(pot: Potential): Effect

    @JvmName("check1")
    fun check(stateOfClass: Checker.StateOfClass): Errors {
        if (stateOfClass.effectsInProcess.contains(this@Effect)) return emptyList()
        stateOfClass.effectsInProcess.add(this@Effect)

        val errors = stateOfClass.check()

        for (error in errors) error.trace.add(this@Effect)

        stateOfClass.effectsInProcess.removeLast()
        return errors
    }

    protected fun Checker.StateOfClass.ruleAcc3(
        effectsAndPotentials: EffectsAndPotentials
    ): Errors =
        effectsAndPotentials.run {
            val errors = potentials.map { createEffectForPotential(it).check(this@ruleAcc3) } // call / select
            val effectErrors = effects.map { it.check(this@ruleAcc3) }
            (errors + effectErrors).flatten()
        }

    fun viewChange(root: Potential): Effect {
        val viewedPot = potential.viewChange(root)
        return createEffectForPotential(viewedPot)
    }
}