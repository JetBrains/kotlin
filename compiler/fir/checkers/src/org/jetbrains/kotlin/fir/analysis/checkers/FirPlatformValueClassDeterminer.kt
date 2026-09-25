/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
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
