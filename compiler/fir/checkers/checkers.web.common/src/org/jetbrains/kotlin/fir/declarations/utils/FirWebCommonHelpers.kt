/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SymbolInternals::class)

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.hasAnnotationOrInsideAnnotatedClass
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

private val FirBasedSymbol<*>.isExternal
    get() = when (this) {
        is FirCallableSymbol<*> -> isExternal
        is FirClassSymbol<*> -> isExternal
        else -> false
    }

/**
 * The containing symbol is resolved using the declaration-site session.
 */
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

    return getContainingClassSymbol()?.isEffectivelyExternal(session) == true
}

fun FirBasedSymbol<*>.isNativeObject(session: FirSession): Boolean {
    if (hasAnnotationOrInsideAnnotatedClass(WebCommonStandardClassIds.Annotations.JsNative, session) || isEffectivelyExternal(session)) {
        return true
    }

    if (this is FirPropertyAccessorSymbol) {
        val property = propertySymbol
        return property.hasAnnotationOrInsideAnnotatedClass(WebCommonStandardClassIds.Annotations.JsNative, session)
    }

    return false
}

fun FirBasedSymbol<*>.isNativeInterface(session: FirSession): Boolean {
    return isNativeObject(session) && (fir as? FirClass)?.isInterface == true
}