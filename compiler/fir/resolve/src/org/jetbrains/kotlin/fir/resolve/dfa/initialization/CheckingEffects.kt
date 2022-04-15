/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.initialization

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.dfa.initialization.Effect.*
import org.jetbrains.kotlin.fir.resolve.dfa.initialization.Potential.*

class CheckingEffects {
}


fun resolveThis(
    clazz: FirClass,
    effsAndPots: EffectsAndPotentials,
    innerClass: FirClass,
): EffectsAndPotentials {
    if (clazz === innerClass) return effsAndPots

    val outerSelection = outerSelection(effsAndPots.potentials, innerClass)
    val outerClass = TODO() // outerClass for innerClass
    return resolveThis(clazz, outerSelection, outerClass)
}

fun resolve(clazz: FirClass, firDeclaration: FirDeclaration): FirClass = TODO()

class ClassInitializationState(val clazz: FirClass) {

    private val safeProperties = mutableSetOf<FirProperty>()
    private val allProperties = clazz.declarations.filterIsInstance<FirProperty>()

    fun potentialPropagation(potential: Potential): EffectsAndPotentials {
        return when (potential) {
            is FieldPotential -> {
                val (pot, field) = potential
                when (pot) {
                    is Root.This -> {                                     // P-Acc1
                        val clazz = resolve(pot.clazz, field)
                        val potentials = pot.potentialsOf(clazz, field)
                        EffectsAndPotentials(listOf(), potentials.viewChange(pot))
                    }
                    is Root.Warm -> {                                         // P-Acc2
                        val clazz = resolve(pot.clazz, field)
                        val potentials = pot.potentialsOf(clazz, field)
                        EffectsAndPotentials(listOf(), potentials.viewChange(pot))
                    }
                    is Root.Cold -> EffectsAndPotentials(Promote(pot)) // or exception or empty list
                    is FunPotential -> throw IllegalArgumentException()
                    else -> {                                                       // P-Acc3
                        val (effects, potentials) = potentialPropagation(pot)
                        val sel = select(potentials, field)
                        EffectsAndPotentials(effects + sel.effects, sel.potentials)
                    }
                }
            }
            is MethodPotential -> {
                val (pot, method) = potential
                when (pot) {
                    is Root.This -> {                                     // P-Inv1
                        val clazz = resolve(pot.clazz, method)
                        val potentials = pot.potentialsOf(clazz, method)
                        EffectsAndPotentials(listOf(), potentials)
                    }
                    is Root.Warm -> {                                     // P-Inv2
                        val clazz = resolve(pot.clazz, method)
                        val potentials = pot.potentialsOf(clazz, method)
                        EffectsAndPotentials(listOf(), potentials.viewChange(pot))
                    }
                    is Root.Cold -> EffectsAndPotentials(Promote(pot))
                    is FunPotential -> {
                        TODO()
                    }
                    else -> {                                                       // P-Inv3
                        val (effects, potentials) = potentialPropagation(pot)
                        val call = call(potentials, method)
                        EffectsAndPotentials(effects + call.effects, call.potentials)
                    }
                }
            }
            is OuterPotential -> {
                val (pot, outer) = potential
                when (pot) {
                    is Root.This -> EffectsAndPotentials()                // P-Out1
                    is Root.Warm -> {                                     // P-Out2
                        TODO()// просто вверх по цепочке наследования
                    }
                    is Root.Cold -> EffectsAndPotentials(Promote(pot))  // or exception or empty list
                    is FunPotential -> throw IllegalArgumentException()
                    else -> {                                                       // P-Out3
                        val (effects, potentials) = potentialPropagation(pot)
                        val out = outerSelection(potentials, outer)
                        EffectsAndPotentials(effects + out.effects, out.potentials)
                    }
                }
            }
            is FunPotential -> TODO() // invoke?
            is Root.Cold, is Root.This, is Root.Warm -> EffectsAndPotentials(potential)
        }
    }

    fun effectChecking(effect: Effect): List<Error> {
        return when (effect) {
            is FieldAccess -> {
                val (pot, field) = effect
                when (pot) {
                    is Root.This -> {                                     // C-Acc1
                        if (safeProperties.contains(field))
                            listOf()
                        else listOf(Error.AccessError())
                    }
                    is Root.Warm -> listOf()                              // C-Acc2
                    is FunPotential -> throw Exception()                  // impossible
                    is Root.Cold -> listOf(Error.AccessError())           // illegal
                    else ->                                                         // C-Acc3
                        ruleAcc3(potentialPropagation(pot)) { p -> FieldAccess(p, field) }
                }
            }
            is MethodAccess -> {
                val (pot, method) = effect
                when (pot) {
                    is Root.This -> {                                     // C-Inv1
                        val clazz = resolve(pot.clazz, method)
                        pot.effectsOf(clazz, method).flatMap(::effectChecking)
                    }
                    is Root.Warm -> {                                     // C-Inv2
                        val clazz = resolve(pot.clazz, method)
                        pot.effectsOf(clazz, method).flatMap { eff ->
                            viewChange(eff, pot)
                            effectChecking(eff)
                        }
                    }
                    is FunPotential -> listOf() // invoke
                    is Root.Cold -> listOf(Error.InvError())              // illegal
                    else ->                                                         // C-Inv3
                        ruleAcc3(potentialPropagation(pot)) { p -> MethodAccess(p, method) }
                }
            }
            is Init -> {                                                     // C-Init
                val (pot, clazz) = effect
                pot.effectsOf(clazz, clazz).flatMap { eff ->
                    viewChange(eff, pot)
                    effectChecking(eff)
                }
                // ???
            }
            is Promote -> {
                val pot = effect.potential
                when (pot) {
                    is Root.Warm -> {                                     // C-Up1
                        pot.clazz.declarations.map {
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
                    is Root.This -> listOf(Error.PromoteError())
                    is FunPotential -> ruleAcc3(pot.effectsAndPotentials, Effect::Promote)
                    is Root.Cold -> listOf(Error.PromoteError())
                    else -> ruleAcc3(potentialPropagation(pot), Effect::Promote)   // C-Up2
                }
            }
        }
    }

    private fun ruleAcc3(effectsAndPotentials: EffectsAndPotentials, producerOfEffects: (Potential) -> Effect): List<Error> =
        effectsAndPotentials.run {
            val errors = potentials.map { effectChecking(producerOfEffects(it)) } // call / select
            val effectErrors = effects.map(::effectChecking)
            (errors + effectErrors).flatten()
        }
}

sealed class Error {
    class AccessError : Error()

    class InvError : Error()

    class PromoteError : Error()
}
