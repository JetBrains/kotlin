/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.Effect
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential

//typealias Effects = List<Effect>

object EmptyEffects : Effects<Effect>(emptyList())

open class Effects<P : Effect>(private val effectList: Collection<P>) : Collection<P> by effectList {

    constructor(effect: P) : this(listOf(effect))

    fun viewChange(root: Potential): Effects<*> = map { eff -> eff.viewChange(root) }.toEffects()

    operator fun plus(effects: Effects<*>) = (effectList + effects.effectList).toEffects()

    fun toEffectsAndPotentials() = EffectsAndPotentials(this)

    companion object {
        fun <P : Effect> Collection<P>.toEffects() = Effects(this)
    }
}

    fun viewChange(root: Potential): Effect {
        val viewedPot = potential.viewChange(root)
        return createEffectForPotential(viewedPot)
    }
}