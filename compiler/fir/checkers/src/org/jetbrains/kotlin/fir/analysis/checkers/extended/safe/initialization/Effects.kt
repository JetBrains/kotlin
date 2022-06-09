/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Effect.*
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potential.Root
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable

typealias Effects = List<Effect>

sealed class Effect(open val potential: Potential) {
    data class Promote(override val potential: Potential) : Effect(potential) {
        override fun toString(): String {
            return "$potential↑"
        }
    }

    data class FieldAccess(override val potential: Potential, val field: FirVariable) : Effect(potential) {
        override fun toString(): String {
            return "$potential.${field.symbol.callableId.callableName}!"
        }
    }

    data class MethodAccess(override val potential: Potential, var method: FirFunction) : Effect(potential) {
        override fun toString(): String {
            return "$potential.${method.symbol.callableId.callableName}◊"
        }
    }

    data class Init(override val potential: Root.Warm, val clazz: FirClass) : Effect(potential) {
        override fun toString(): String {
            return "$potential.init(${clazz.symbol.classId.shortClassName})"
        }
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
