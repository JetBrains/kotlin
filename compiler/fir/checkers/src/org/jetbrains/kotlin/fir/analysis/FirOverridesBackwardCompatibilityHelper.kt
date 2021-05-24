/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

interface FirOverridesBackwardCompatibilityHelper : FirSessionComponent {
    fun overrideCanBeOmitted(overriddenMemberSymbols: List<FirCallableSymbol<*>>, context: CheckerContext): Boolean

    class Default : FirOverridesBackwardCompatibilityHelper {
        override fun overrideCanBeOmitted(overriddenMemberSymbols: List<FirCallableSymbol<*>>, context: CheckerContext): Boolean = false
    }
}

val FirSession.overridesBackwardCompatibilityHelper: FirOverridesBackwardCompatibilityHelper by FirSession.sessionComponentAccessor()
