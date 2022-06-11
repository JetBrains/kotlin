/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Super
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Warm
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirVariable

typealias Potentials = List<Potential>

inline fun <reified T : FirMemberDeclaration> Checker.StateOfClass.resolveMember(potential: Potential, dec: T): T =
    if (potential is Super) dec else overriddenMembers.getOrDefault(dec, dec) as T

fun Checker.StateOfClass.select(potentials: Potentials, field: FirVariable): EffectsAndPotentials {
    return potentials.fold(emptyEffsAndPots) { sum, pot -> sum + pot.run { select(field) } }
}

fun Checker.StateOfClass.call(potentials: Potentials, function: FirFunction): EffectsAndPotentials {
    return potentials.fold(emptyEffsAndPots) { sum, pot -> sum + pot.run { call(function) } }
}

fun outerSelection(potentials: Potentials, clazz: FirClass): EffectsAndPotentials {
    return potentials.fold(emptyEffsAndPots) { sum, pot -> sum + pot.run { outerSelection(clazz) } }
}

fun promote(potentials: Potentials): Effects = potentials.map(::Promote)

fun init(
    potentials: Potentials,
    clazz: FirClass,
): EffectsAndPotentials {
    val prefixPotentials = potentials.map { pot -> Warm(clazz, pot) }
    val initEffects = prefixPotentials.map { warm -> Init(warm, clazz) }

    return EffectsAndPotentials(initEffects, prefixPotentials)
}

fun Potentials.viewChange(root: Potential): Potentials = map { pot -> pot.viewChange(root) }