/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.initialization

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.references.FirThisReference


sealed class Potential(val firElement: FirElement, val length: Int = 0) {

    sealed class Root(firElement: FirElement, length: Int = 0) : Potential(firElement, length) {

        fun effectsOf(firElement: FirElement): List<Effect> = TODO()
        fun potentialsOf(firElement: FirElement): List<Potential> = TODO()

        class This(firElement: FirThisReference) : Root(firElement)
        class Warm(firElement: FirElement, val clazz: FirClass, val outer: Potential) : Root(firElement, outer.length + 1)
        class Cold(firElement: FirElement) : Root(firElement)
    }

    class MethodPotential(firElement: FirElement, val potential: Potential, val method: FirFunction) :
        Potential(firElement, potential.length + 1)

    class FieldPotential(firElement: FirElement, val potential: Potential, val field: FirProperty) :
        Potential(firElement, potential.length + 1)

    class OuterPotential(firElement: FirElement, val potential: Potential, val outerClass: FirClass) :
        Potential(firElement, potential.length + 1)

    class FunPotential(firElement: FirElement, val potential: Potential, val todo: Any) : Potential(firElement, potential.length + 1)
}

fun select(potentials: List<Potential>, firElement: FirElement, field: FirProperty): EffectsAndPotentials {

    fun select(potential: Potential, firElement: FirElement, field: FirProperty): EffectsAndPotentials = when {
        field.initializer != null -> EffectsAndPotentials()
        potential is Potential.Root.Cold -> EffectsAndPotentials(Effect.Propagate(potential))
        potential.length < 2 -> EffectsAndPotentials(
            Effect.FieldAccess(potential, field),
            Potential.FieldPotential(firElement, potential, field)
        )
        else -> EffectsAndPotentials(Effect.Propagate(potential))
    }

    return potentials.fold(EffectsAndPotentials()) { sum, pot -> sum.addEffectsAndPotentials(select(pot, firElement, field)) }
}

fun call(potentials: List<Potential>, firElement: FirElement, function: FirFunction): EffectsAndPotentials {

    fun call(potential: Potential, firElement: FirElement, function: FirFunction): EffectsAndPotentials = when {
        potential is Potential.Root.Cold -> EffectsAndPotentials(Effect.Propagate(potential))
        potential.length < 2 -> EffectsAndPotentials(
            Effect.MethodAccess(potential, function),
            Potential.MethodPotential(firElement, potential, function)
        )
        else -> EffectsAndPotentials(Effect.Propagate(potential))
    }

    return potentials.fold(EffectsAndPotentials()) { sum, pot -> sum.addEffectsAndPotentials(call(pot, firElement, function)) }
}

fun outerSelection(potentials: List<Potential>, firElement: FirElement, clazz: FirClass): EffectsAndPotentials {

    fun outerSelection(potential: Potential, clazz: FirClass): EffectsAndPotentials = when {
        potential is Potential.Root.Cold -> EffectsAndPotentials(Effect.Propagate(potential))
        potential.length < 2 -> EffectsAndPotentials(Potential.OuterPotential(firElement, potential, clazz))
        else -> EffectsAndPotentials(Effect.Propagate(potential))
    }

    return potentials.fold(EffectsAndPotentials()) { sum, pot -> sum.addEffectsAndPotentials(outerSelection(pot, clazz)) }
}

fun propagate(potentials: List<Potential>): List<Effect> = potentials.map(Effect::Propagate)

fun init(
    clazz: FirClass,
    fieldsPots: List<List<Potential>>,
    prefix: FirElement,
    potentials: List<Potential> = listOf()
): EffectsAndPotentials {
    val propagateEffects = fieldsPots.flatMap(::propagate)
    val prefixPotentials = potentials.map { pot -> Potential.Root.Warm(prefix, clazz, pot) }
    val initEffects = prefixPotentials.map { warm -> Effect.Init(warm, clazz) }

    return EffectsAndPotentials(propagateEffects + initEffects, prefixPotentials)
}

fun List<Potential>.viewChange(root: Potential): List<Potential> = map { pot -> viewChange(pot, root) }

fun viewChange(potential: Potential, root: Potential): Potential =
    when (potential) {
        is Potential.Root.This -> root
        is Potential.Root.Cold -> potential
        is Potential.Root.Warm -> {
            if (potential.outer is Potential.Root.Cold) return TODO()
            val viewedPot = viewChange(potential.outer, root)
            Potential.Root.Warm(potential.firElement, potential.clazz, viewedPot)
        }
        is Potential.FieldPotential -> TODO()
        is Potential.FunPotential -> TODO()
        is Potential.MethodPotential -> TODO()
        is Potential.OuterPotential -> TODO()
    }