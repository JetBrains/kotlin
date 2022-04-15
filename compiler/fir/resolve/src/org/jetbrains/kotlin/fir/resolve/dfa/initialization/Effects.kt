/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.initialization

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.resolve.dfa.initialization.Effect.*
import org.jetbrains.kotlin.fir.resolve.dfa.initialization.Potential.*

sealed class Effect {
    data class Promote(val potential: Potential) : Effect()
    data class FieldAccess(val potential: Potential, val field: FirProperty) : Effect()
    data class MethodAccess(val potential: Potential, var method: FirFunction) : Effect()
    data class Init(val potential: Root.Warm, val clazz: FirClass) : Effect()
}

fun viewChange(effect: Effect, root: Potential): Effect {
    return when (effect) {
        is Promote -> {
            val viewedPot = viewChange(effect.potential, root)
            Promote(viewedPot)
        }
        is FieldAccess -> {
            val viewedPot = viewChange(effect.potential, root)
            FieldAccess(viewedPot, effect.field)
        }
        is MethodAccess -> {
            val viewedPot = viewChange(effect.potential, root)
            MethodAccess(viewedPot, effect.method)
        }
        is Init -> {
            val viewedPot = viewChange(effect.potential, root) as Root.Warm
            Init(viewedPot, effect.clazz)
        }
    }
}
