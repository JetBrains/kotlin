/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SymbolInternals::class)

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.isEffectivelyExternal
import org.jetbrains.kotlin.fir.resolve.isEffectivelyExternalMember
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.types.getContainingClassSymbol
import org.jetbrains.kotlin.name.StandardClassIds

fun FirBasedSymbol<*>.isNativeObject(session: FirSession): Boolean {
    if (hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.JsNative, session) || isEffectivelyExternal(session)) {
        return true
    }

    if (this is FirPropertyAccessorSymbol) {
        val property = propertySymbol ?: error("Should've had a property")
        return property.hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.JsNative, session)
    }

    return if (this is FirAnonymousInitializerSymbol) {
        getContainingClassSymbol(session)?.isNativeObject(session) == true
    } else {
        false
    }
}

val PREDEFINED_ANNOTATIONS = setOf(
    StandardClassIds.Annotations.JsLibrary,
    StandardClassIds.Annotations.JsNative,
    StandardClassIds.Annotations.JsNativeInvoke,
    StandardClassIds.Annotations.JsNativeGetter,
    StandardClassIds.Annotations.JsNativeSetter
)

private val FirBasedSymbol<*>.isExpect
    get() = when (this) {
        is FirCallableSymbol<*> -> isExpect
        is FirClassSymbol<*> -> isExpect
        else -> false
    }

fun FirBasedSymbol<*>.isPredefinedObject(session: FirSession): Boolean {
    if (fir is FirMemberDeclaration && isExpect) return true
    if (isEffectivelyExternalMember(session)) return true

    for (annotation in PREDEFINED_ANNOTATIONS) {
        if (hasAnnotationOrInsideAnnotatedClass(annotation, session)) {
            return true
        }
    }

    return false
}
