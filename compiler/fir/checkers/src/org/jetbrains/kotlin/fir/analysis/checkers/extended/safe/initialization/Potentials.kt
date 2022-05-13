/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.ClassAnalyser.analyseDeclaration
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potential.*
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization._Effect.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.references.FirThisReference

typealias Potentials = List<Potential>

sealed class Potential(val firElement: FirElement, val length: Int = 0) {

    sealed class Root(firElement: FirElement, length: Int = 0) : Potential(firElement, length) {

        fun effectsOf(state: Checker.StateOfClass, firDeclaration: FirDeclaration): Effects =
            state.analyseDeclaration(firDeclaration).effects

        fun potentialsOf(state: Checker.StateOfClass, firDeclaration: FirDeclaration): Potentials =
            state.analyseDeclaration(firDeclaration).potentials

        data class This(val firThisReference: FirThisReference) : Root(firThisReference) {
            override fun toString(): String {
                return "This(class=this@${firThisReference.labelName})"
            }
        }

        data class Warm(val clazz: FirClass, val outer: Potential) : Root(clazz, outer.length + 1)
        data class Cold(val firDeclaration: FirDeclaration) : Root(firDeclaration)
    }

    data class MethodPotential(val potential: Potential, val method: FirFunction) :
        Potential(method, potential.length + 1)

    data class FieldPotential(val potential: Potential, val field: FirVariable) :
        Potential(field, potential.length + 1)

    data class OuterPotential(val potential: Potential, val outerClass: FirClass) :
        Potential(outerClass, potential.length + 1)

    data class FunPotential(
        val effectsAndPotentials: EffectsAndPotentials,
        val anonymousFunction: FirAnonymousFunction
    ) : Potential(anonymousFunction, effectsAndPotentials.maxLength())
}

fun select(potentials: Potentials, field: FirVariable): EffectsAndPotentials {

    fun select(potential: Potential, field: FirVariable): EffectsAndPotentials = when {
        field is FirValueParameter -> emptyEffsAndPots
        potential is Root.Cold -> EffectsAndPotentials(Promote(potential))
        potential.length < 2 -> EffectsAndPotentials(
            FieldAccess(potential, field),
            FieldPotential(potential, field)
        )
        else -> EffectsAndPotentials(Promote(potential))
    }

    return potentials.fold(emptyEffsAndPots) { sum, pot -> sum + select(pot, field) }
}

fun call(potentials: Potentials, function: FirFunction): EffectsAndPotentials {

    fun call(potential: Potential, function: FirFunction): EffectsAndPotentials = when {
        potential is Root.Cold -> EffectsAndPotentials(Promote(potential))
        potential.length < 2 -> EffectsAndPotentials(
            MethodAccess(potential, function),
            MethodPotential(potential, function)
        )
        else -> EffectsAndPotentials(Promote(potential))
    }

    return potentials.fold(emptyEffsAndPots) { sum, pot -> sum + call(pot, function) }
}

fun outerSelection(potentials: Potentials, clazz: FirClass): EffectsAndPotentials {

    fun outerSelection(potential: Potential, clazz: FirClass): EffectsAndPotentials = when {
        potential is Root.Cold -> EffectsAndPotentials(Promote(potential))
        potential.length < 2 -> EffectsAndPotentials(OuterPotential(potential, clazz))
        else -> EffectsAndPotentials(Promote(potential))
    }

    return potentials.fold(emptyEffsAndPots) { sum, pot -> sum + outerSelection(pot, clazz) }
}

fun promote(potentials: Potentials): Effects = potentials.map(::Promote)

fun init(
    clazz: FirClass,
    fieldsPots: List<Potentials>,
    potentials: Potentials = emptyList()
): EffectsAndPotentials {
    val propagateEffects = fieldsPots.flatMap(::promote)
    val prefixPotentials = potentials.map { pot -> Root.Warm(clazz, pot) }
    val initEffects = prefixPotentials.map { warm -> Init(warm, clazz) }

    return EffectsAndPotentials(propagateEffects + initEffects, prefixPotentials)
}

fun Potentials.viewChange(root: Potential): Potentials = map { pot -> viewChange(pot, root) }

fun viewChange(potential: Potential, root: Potential): Potential {

    fun asPotSimpleRule(
        pot: Potential,
        potentialConstructor: (Potential) -> Potential
    ): Potential {
        val viewedPot = viewChange(pot, root)
        return potentialConstructor(viewedPot)
    }

    return when (potential) {
        is Root.This -> root                                              // As-Pot-This
        is Root.Cold -> potential                                         // As-Pot-Cold
        is Root.Warm -> potential.run {
            when { // ???
                outer is Root.Cold -> potential
                outer.length < 2 -> Root.Warm(clazz, Root.Cold(clazz)) // ???
                else -> {
                    val viewedPot = viewChange(outer, root)
                    Root.Warm(clazz, viewedPot)
                }
            }
        }
        is FieldPotential -> {                                            // As-Pot-Acc
            val (pot, field) = potential
            val viewedPot = viewChange(pot, root)
            FieldPotential(viewedPot, field)
        }
        is MethodPotential -> {                                           // As-Pot-Inv
            val (pot, method) = potential
            val viewedPot = viewChange(pot, root)
            MethodPotential(viewedPot, method)
        }
        is OuterPotential -> {                                            // As-Pot-Out
            asPotSimpleRule(potential.potential) { pot -> OuterPotential(pot, potential.outerClass) }
        }
        is FunPotential -> TODO()
    }
}