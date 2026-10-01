/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

/**
 * Determines the platform classes that are value classes in this compilation although their declarations are read as identity classes,
 * and the types whose instances are value objects at run time.
 */
abstract class FirPlatformValueClassDeterminer : FirSessionComponent {
    abstract fun isPlatformValueClass(symbol: FirRegularClassSymbol): Boolean

    // Reference equality compares such instances by their state.
    abstract fun instancesAreValueObjects(type: ConeKotlinType): Boolean

    object Default : FirPlatformValueClassDeterminer() {
        override fun isPlatformValueClass(symbol: FirRegularClassSymbol): Boolean = false

        override fun instancesAreValueObjects(type: ConeKotlinType): Boolean = false
    }
}

val FirSession.platformValueClassDeterminer: FirPlatformValueClassDeterminer
        by FirSession.sessionComponentAccessorWithDefault(FirPlatformValueClassDeterminer.Default)

// Declared with the `value` modifier in Java, unlike the JDK classes that are value classes only with a Valhalla-compatible JVM target.
@OptIn(SymbolInternals::class)
val FirRegularClassSymbol.isDeclaredJavaValueClass: Boolean
    get() = fir.isJavaValueClass == true

fun FirRegularClassSymbol.isJavaValueClass(session: FirSession): Boolean =
    isDeclaredJavaValueClass || session.platformValueClassDeterminer.isPlatformValueClass(this)
