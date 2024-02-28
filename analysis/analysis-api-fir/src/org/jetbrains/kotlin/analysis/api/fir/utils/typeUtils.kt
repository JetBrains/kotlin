/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

/**
 * Returns whether [subClass] is a strict subtype of [superClass]. Resolves [subClass] to [FirResolvePhase.SUPER_TYPES].
 */
fun isSubClassOf(subClass: FirClass, superClass: FirClass, allowIndirectSubtyping: Boolean = true): Boolean {
    subClass.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)

    val session = subClass.moduleData.session
    if (subClass.superConeTypes.any { it.toRegularClassSymbol(session) == superClass.symbol }) return true
    if (!allowIndirectSubtyping) return false

    subClass.superConeTypes.forEach { superType ->
        val superOfSub = superType.toRegularClassSymbol(session) ?: return@forEach
        if (isSubClassOf(superOfSub.fir, superClass, allowIndirectSubtyping = true)) return true
    }
    return false
}
