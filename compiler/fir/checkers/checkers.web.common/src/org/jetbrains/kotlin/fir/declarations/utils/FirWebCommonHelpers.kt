/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SymbolInternals::class)

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

private val FirBasedSymbol<*>.isExternal
    get() = when (this) {
        is FirCallableSymbol<*> -> isExternal
        is FirClassSymbol<*> -> isExternal
        else -> false
    }

fun FirBasedSymbol<*>.isEffectivelyExternal(session: FirSession): Boolean {
    if (fir is FirMemberDeclaration && isExternal) return true

    if (this is FirPropertyAccessorSymbol) {
        val property = propertySymbol
        if (property.isEffectivelyExternal(session)) return true
    }

    if (this is FirPropertySymbol) {
        if (getterSymbol?.isExternal == true && (!isVar || setterSymbol?.isExternal == true)) {
            return true
        }
    }

    return getContainingClassSymbol(session)?.isEffectivelyExternal(session) == true
}