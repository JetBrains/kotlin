/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.initialization

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty

sealed class Effect {
    data class Propagate(val potential: Potential) : Effect()
    data class FieldAccess(val potential: Potential, val field: FirProperty) : Effect()
    data class MethodAccess(val potential: Potential, var method: FirFunction) : Effect()
    data class Init(val potential: Potential, val clazz: FirClass) : Effect()
}

fun List<Effect>.viewChange(effect: Effect): List<Effect> = TODO()