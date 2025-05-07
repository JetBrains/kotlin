/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

/**
 * The provider of hidden deprecations.
 *
 * It is necessary during the transition period of KT-77114 issue to provide an ability to adjust the behaviour
 * for the Analysis API.
 */
open class FirHiddenDeprecationProvider(val session: FirSession) : FirSessionComponent {
    open fun isDeprecationLevelHidden(symbol: FirBasedSymbol<*>): Boolean = when (symbol) {
        is FirCallableSymbol<*> -> symbol.getDeprecation(session.languageVersionSettings)?.all?.deprecationLevel == DeprecationLevelValue.HIDDEN
        is FirClassLikeSymbol<*> -> symbol.getOwnDeprecation(session.languageVersionSettings)?.all?.deprecationLevel == DeprecationLevelValue.HIDDEN
        else -> false
    }
}

val FirSession.hiddenDeprecationProvider: FirHiddenDeprecationProvider by FirSession.sessionComponentAccessor()