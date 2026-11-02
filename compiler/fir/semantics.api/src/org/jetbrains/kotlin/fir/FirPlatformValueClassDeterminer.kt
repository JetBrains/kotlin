/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

/**
 * Determines the value-like classes of the platform: its value classes, like Java value classes and the JDK classes that are value classes
 * with the current target, and its value-based classes, whose identity must not be relied upon. By default, there are none.
 */
abstract class FirPlatformValueClassDeterminer : FirSessionComponent {
    abstract fun isPlatformValueClass(symbol: FirRegularClassSymbol): Boolean

    abstract fun isValueBasedClass(symbol: FirRegularClassSymbol): Boolean

    object Default : FirPlatformValueClassDeterminer() {
        override fun isPlatformValueClass(symbol: FirRegularClassSymbol): Boolean = false

        override fun isValueBasedClass(symbol: FirRegularClassSymbol): Boolean = false
    }
}

val FirSession.platformValueClassDeterminer: FirPlatformValueClassDeterminer
        by FirSession.sessionComponentAccessorWithDefault(FirPlatformValueClassDeterminer.Default)

/**
 * Whether this class is declared as a value class in Java, unlike the JDK classes that are value classes only with a Valhalla-compatible
 * JVM target.
 */
@OptIn(SymbolInternals::class)
val FirRegularClassSymbol.isJavaValueClass: Boolean
    get() = fir.isJavaValueClass == true

/**
 * Whether this class is a value class of the platform in this compilation, like a Java value class or a JDK class with JVM target 28 and
 * preview features ([FirPlatformValueClassDeterminer]).
 *
 * Only the class itself is checked, unlike `ConeKotlinType.isValueClass` in the checkers, which also looks through type-parameter bounds.
 */
fun FirRegularClassSymbol.isPlatformValueClass(session: FirSession): Boolean =
    session.platformValueClassDeterminer.isPlatformValueClass(this)

/**
 * Whether this class is a value-based class of the platform, whose identity must not be relied upon, like a JDK class annotated with
 * `@jdk.internal.ValueBased` ([FirPlatformValueClassDeterminer]).
 */
fun FirRegularClassSymbol.isValueBasedClass(session: FirSession): Boolean =
    session.platformValueClassDeterminer.isValueBasedClass(this)
