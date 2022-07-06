/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.StateOfClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Effects.Companion.toEffects
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials.Companion.emptyEffsAndPots
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.Init
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.Promote
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Warm
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
import kotlin.collections.plus as collectionsPlus

object EmptyPotentials : Potentials(emptyList())

open class Potentials(protected val collection: Collection<Potential>) : Collection<Potential> by collection {

    constructor(potential: Potential) : this(listOf(potential))

    fun select(stateOfClass: StateOfClass, field: FirVariable) =
        fold(emptyEffsAndPots) { sum, pot -> sum + pot.select(stateOfClass, field) }

    fun call(stateOfClass: StateOfClass, function: FirFunction): EffectsAndPotentials =
        fold(emptyEffsAndPots) { sum, pot -> sum + pot.call(stateOfClass, function) }

    fun outerSelection(clazz: FirClass): EffectsAndPotentials =
        fold(emptyEffsAndPots) { sum, pot -> sum + pot.outerSelection(clazz) }

    fun wrapPots(createPot: (Potential) -> Potential) = map { createPot(it) }.fastToPotentials()

    fun promote() = map(::Promote).toEffects()

    fun viewChange(root: Potential) =
        map { pot -> pot.viewChange(root) }.toPotentials()

    fun init(firConstructorSymbol: FirConstructorSymbol): EffectsAndPotentials {
        @OptIn(LookupTagInternals::class)
        fun FirConstructorSymbol.getClassFromConstructor() =
            containingClass()?.toFirRegularClass(moduleData.session)

        val clazz = firConstructorSymbol.getClassFromConstructor() ?: return emptyEffsAndPots

        return fold(emptyEffsAndPots) { sum, pot ->
            val warmPot = Warm(clazz, pot)
            val initEff = Init(warmPot, clazz, firConstructorSymbol)
            sum + warmPot + initEff
        }
    }

    operator fun plus(potential: Potential) = collectionsPlus(potential).fastToPotentials()

    operator fun plus(collection: Collection<Potential>) =
        collectionsPlus(collection).fastToPotentials()

    operator fun plus(effsAndPots: EffectsAndPotentials): EffectsAndPotentials = effsAndPots + this

    companion object {
        private fun Collection<Potential>.fastToPotentials() = Potentials(this)

        fun Collection<Potential>.toPotentials() = Potentials(toList())
    }
}