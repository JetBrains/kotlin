/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.abbreviatedType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

/**
 * Returns whether [subClass] is a strict subtype of [superClass]. Resolves [subClass] to [FirResolvePhase.SUPER_TYPES].
 */
fun isSubClassOf(subClass: FirClass, superClass: FirClass, useSiteSession: FirSession, allowIndirectSubtyping: Boolean = true): Boolean {
    subClass.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)

    if (subClass.superConeTypes.any { it.toRegularClassSymbol(useSiteSession) == superClass.symbol }) return true
    if (!allowIndirectSubtyping) return false

    subClass.superConeTypes.forEach { superType ->
        val superOfSub = superType.toRegularClassSymbol(useSiteSession) ?: return@forEach
        if (isSubClassOf(superOfSub.fir, superClass, useSiteSession, allowIndirectSubtyping = true)) return true
    }
    return false
}

/**
 * @see org.jetbrains.kotlin.analysis.api.types.KaType.abbreviatedType
 */
internal fun KaSymbolByFirBuilder.buildAbbreviatedType(coneType: ConeClassLikeType): KaUsualClassType? {
    return coneType.abbreviatedType?.let { abbreviatedConeType ->
        // If the resulting type is an error type, the abbreviated type couldn't be resolved. As per the contract of
        // `KaType.abbreviatedType`, we should return `null` in such cases. The user can then fall back to the expanded type.
        typeBuilder.buildKtType(abbreviatedConeType) as? KaUsualClassType
    }
}
