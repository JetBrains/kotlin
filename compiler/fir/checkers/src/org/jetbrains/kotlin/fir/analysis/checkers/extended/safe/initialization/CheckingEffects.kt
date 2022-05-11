/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Effect.*
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potential.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment

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

fun resolve(clazz: FirClass, firDeclaration: FirDeclaration): FirClass = clazz // maybe delete

object Checker {

    data class StateOfClass(val firClass: FirClass) {
        val alreadyInitializedVariable = mutableSetOf<FirVariable>()
        val maybeUninitializedProperties = run {
            val properties = firClass.declarations.filterIsInstance<FirProperty>()
            properties.associateWith { emptyEffsAndPots }
        }

        val notFinalAssignments = mutableMapOf<FirProperty, Set<FirVariableAssignment>>()
        val caches = mutableMapOf<FirDeclaration, EffectsAndPotentials>()
    }


    fun StateOfClass.checkClass(): List<List<Error>> =
        firClass.declarations.map { dec ->
            when (dec) {
                is FirConstructor -> {
                    if (dec.isPrimary)
                        alreadyInitializedVariable + dec.valueParameters
                    checkBody(dec)
                }
                is FirAnonymousInitializer -> {
                    dec.body?.let(::analyser)
                    TODO()
                }
                is FirRegularClass -> {
                    val state = StateOfClass(dec)
                    val errors = state.checkClass().flatten()
                    errors
                }
                is FirPropertyAccessor -> TODO()
//                is FirSimpleFunction -> checkBody(dec)
                is FirField -> TODO()
                is FirProperty -> {
                    val (effs, _) = dec.initializer?.let(::analyser) ?: throw IllegalArgumentException()
                    val errors = effs.flatMap { effectChecking(it) }
                    alreadyInitializedVariable.add(dec)
                    errors
                }
                else -> emptyList()
            }
        }
    
    fun StateOfClass.checkBody(dec: FirFunction): List<Error> {
        val (effs, _) = dec.body?.let(::analyser) ?: emptyEffsAndPots
        return effs.flatMap { effectChecking(it) }
    }

    fun StateOfClass.potentialPropagation(potential: Potential): EffectsAndPotentials {
        return when (potential) {
            is FieldPotential -> {
                val (pot, field) = potential
                when (pot) {
                    is Root.This -> {                                     // P-Acc1
                        val clazz = resolve(pot.clazz, field)
                        val potentials = pot.potentialsOf(this, field)
                        EffectsAndPotentials(potentials = potentials.viewChange(pot))
                    }
                    is Root.Warm -> {                                         // P-Acc2
                        val clazz = resolve(pot.clazz, field)
                        val potentials = pot.potentialsOf(this, field)
                        EffectsAndPotentials(potentials = potentials.viewChange(pot))
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
                        val potentials = pot.potentialsOf(this, method)
                        EffectsAndPotentials(emptyList(), potentials)
                    }
                    is Root.Warm -> {                                     // P-Inv2
                        val clazz = resolve(pot.clazz, method)
                        val potentials = pot.potentialsOf(this, method)
                        EffectsAndPotentials(emptyList(), potentials.viewChange(pot))
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
                    is Root.This -> emptyEffsAndPots                // P-Out1
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

    fun StateOfClass.effectChecking(effect: Effect): List<Error> {
        return when (effect) {
            is FieldAccess -> {
                val (pot, field) = effect
                when (pot) {
                    is Root.This -> {                                     // C-Acc1
                        if (alreadyInitializedVariable.contains(field))
                            emptyList()
                        else listOf(Error.AccessError(field))
                    }
                    is Root.Warm -> emptyList()                              // C-Acc2
                    is FunPotential -> throw Exception()                  // impossible
                    is Root.Cold -> listOf(Error.AccessError(field))           // illegal
                    else ->                                                         // C-Acc3
                        ruleAcc3(potentialPropagation(pot)) { p -> FieldAccess(p, field) }
                }
            }
            is MethodAccess -> {
                val (pot, method) = effect
                when (pot) {
                    is Root.This -> {                                     // C-Inv1
                        val clazz = resolve(pot.clazz, method)
                        pot.effectsOf(this, method).flatMap { effectChecking(it) }
                    }
                    is Root.Warm -> {                                     // C-Inv2
                        val clazz = resolve(pot.clazz, method)
                        pot.effectsOf(this, method).flatMap { eff ->
                            viewChange(eff, pot)
                            effectChecking(eff)
                        }
                    }
                    is FunPotential -> emptyList() // invoke
                    is Root.Cold -> listOf(Error.InvError())              // illegal
                    else ->                                                         // C-Inv3
                        ruleAcc3(potentialPropagation(pot)) { p -> MethodAccess(p, method) }
                }
            }
            is Init -> {                                                     // C-Init
                val (pot, clazz) = effect
                pot.effectsOf(this, clazz).flatMap { eff ->
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
                    is Root.This -> listOf(Error.PromoteError(pot))
                    is FunPotential -> ruleAcc3(pot.effectsAndPotentials, Effect::Promote)
                    is Root.Cold -> listOf(Error.PromoteError(pot))
                    else -> {
                        ruleAcc3(potentialPropagation(pot), Effect::Promote)   // C-Up2

                    }
                }
            }
        }
    }

    private fun StateOfClass.ruleAcc3(effectsAndPotentials: EffectsAndPotentials, producerOfEffects: (Potential) -> Effect): List<Error> =
        effectsAndPotentials.run {
            val errors = potentials.map { effectChecking(producerOfEffects(it)) } // call / select
            val effectErrors = effects.map { effectChecking(it) }
            (errors + effectErrors).flatten()
        }
}

sealed class Error {
    class AccessError(val firProperty: FirVariable) : Error() {
        override fun toString(): String {
            return "AccessError(property=${firProperty.name})"
        }
    }

    class InvError : Error()

    class PromoteError(val potential: Potential) : Error() {
        override fun toString(): String {
            return "PromoteError(potential=${potential})"
        }
    }
}
