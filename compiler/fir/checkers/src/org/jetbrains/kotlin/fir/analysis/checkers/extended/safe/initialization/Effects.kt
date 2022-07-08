/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.Effect
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Potential
import kotlin.collections.plus as collectionsPlus

object EmptyEffects : Effects(emptyList())

open class Effects(private val collection: Collection<Effect>) : Collection<Effect> by collection {

    constructor(effect: Effect) : this(listOf(effect))

    fun viewChange(root: Potential) = map { eff -> eff.viewChange(root) }.fastToEffects()

    operator fun plus(effects: Collection<Effect>) = collectionsPlus(effects).fastToEffects()

    operator fun plus(effect: Effect) = collectionsPlus(effect).fastToEffects()

    operator fun plus(effsAndPots: EffectsAndPotentials): EffectsAndPotentials = effsAndPots + this
    override fun toString(): String =
        "Φ=$collection"


    companion object {
        private fun Collection<Effect>.fastToEffects() = Effects(this)

        fun Collection<Effect>.toEffects() = Effects(toList())
    }
}