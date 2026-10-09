/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

/**
 * Determines the platform classes that are value classes in this compilation although their declarations are read as identity classes.
 */
abstract class FirPlatformValueClassDeterminer : FirSessionComponent {
    abstract fun isPlatformValueClass(symbol: FirRegularClassSymbol): Boolean

    object Default : FirPlatformValueClassDeterminer() {
        override fun isPlatformValueClass(symbol: FirRegularClassSymbol): Boolean = false
    }
}

val FirSession.platformValueClassDeterminer: FirPlatformValueClassDeterminer
        by FirSession.sessionComponentAccessorWithDefault(FirPlatformValueClassDeterminer.Default)

/**
 * Whether this class is declared as a value class in Java, unlike the JDK classes that are value classes only with a Valhalla-compatible
 * JVM target.
 */
@OptIn(SymbolInternals::class)
val FirRegularClassSymbol.isDeclaredJavaValueClass: Boolean
    get() = fir.isJavaValueClass == true

/**
 * Whether this class is a Java value class in this compilation: declared as a value class in Java ([isDeclaredJavaValueClass]), or a JDK
 * class that is a value class with the current JVM target and preview features ([FirPlatformValueClassDeterminer]).
 *
 * Only the class itself is checked, unlike `ConeKotlinType.isValueClass` in the checkers, which also looks through type-parameter bounds.
 */
fun FirRegularClassSymbol.isJavaValueClass(session: FirSession): Boolean =
    isDeclaredJavaValueClass || session.platformValueClassDeterminer.isPlatformValueClass(this)
