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

    override fun hashCode(): Int {
        return potential.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is _Effect<*>) return false

        if (potential != other.potential) return false

        return true
    }

    @Suppress("EqualsOrHashCode")
    class Promote(potential: Potential) : _Effect<Potential>(potential) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Promote) return false
            if (!super.equals(other)) return false
            return true
        }
    }

    class FieldAccess(potential: Potential, val field: FirVariable) : _Effect<Potential>(potential) {
        operator fun component2() = field

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FieldAccess) return false
            if (!super.equals(other)) return false

            if (field != other.field) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + field.hashCode()
            return result
        }

    }

    class MethodAccess(potential: Potential, var method: FirFunction) : _Effect<Potential>(potential) {
        operator fun component2() = method

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MethodAccess) return false
            if (!super.equals(other)) return false

            if (method != other.method) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + method.hashCode()
            return result
        }
    }

    class Init(potential: Root.Warm, val clazz: FirClass) : _Effect<Root.Warm>(potential) {
        operator fun component2() = clazz

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Init) return false
            if (!super.equals(other)) return false

            if (clazz != other.clazz) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + clazz.hashCode()
            return result
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
