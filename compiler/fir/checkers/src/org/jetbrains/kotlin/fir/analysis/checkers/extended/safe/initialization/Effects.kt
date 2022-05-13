/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potential.Root
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization._Effect.*
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable

typealias Effects = List<Effect>

typealias Effect = _Effect<*>

sealed class _Effect<P : Potential>(val potential: P) {

    operator fun component1() = potential

    class Promote(potential: Potential) : _Effect<Potential>(potential)
    class FieldAccess(potential: Potential, val field: FirVariable) : _Effect<Potential>(potential) {
        operator fun component2() = field
    }

    class MethodAccess(potential: Potential, var method: FirFunction) : _Effect<Potential>(potential) {
        operator fun component2() = method
    }

    class Init(potential: Root.Warm, val clazz: FirClass) : _Effect<Root.Warm>(potential) {
        operator fun component2() = clazz
    }
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
