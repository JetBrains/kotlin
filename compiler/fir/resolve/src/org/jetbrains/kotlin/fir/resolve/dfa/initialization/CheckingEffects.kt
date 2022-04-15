/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.initialization

import org.jetbrains.kotlin.fir.declarations.*

class CheckingEffects {
}

fun resolveThis(
    clazz: FirClass,
    potential: List<Potential>,
    effect: Effect,
    innerClass: FirClass
): EffectsAndPotentials = TODO()

fun resolve(clazz: FirClass, firDeclaration: FirDeclaration): FirClass = TODO()

fun potentialPropagation(): EffectsAndPotentials = TODO()

fun propagationChecking(propagate: Effect.Propagate) {
    when (propagate.potential) {
        // TODO: cache
        is Potential.Root.This -> TODO()
        is Potential.Root.Cold -> throw Exception()
        is Potential.Root.Warm -> {
            val warm = propagate.potential
            val clazz = warm.clazz
            for (dec in warm.clazz.declarations) {
                when (dec) {
                    else -> TODO()
                }
            }
        }
        is Potential.FieldPotential -> TODO()
        is Potential.FunPotential -> TODO()
        is Potential.MethodPotential -> TODO()
        is Potential.OuterPotential ->TODO()
    }
}
