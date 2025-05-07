/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirHiddenDeprecationProvider
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * Due to unpredictable Java resolution behavior, the Analysis API need to disable [FirHiddenDeprecationProvider] for Java sources.
 * Otherwise, it might cause a contract violation as this check is called during the
 * [TYPES][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.TYPES] phase.
 *
 * After [KT-77114](https://youtrack.jetbrains.com/issue/KT-77114) the provider won't be needed anymore.
 */
class LLHiddenDeprecationProvider(session: FirSession) : FirHiddenDeprecationProvider(session) {
    override fun isDeprecationLevelHidden(symbol: FirBasedSymbol<*>): Boolean {
        val fir = symbol.fir
        if (fir is FirJavaClass && fir.origin.fromSource) {
            return false
        }

        return super.isDeprecationLevelHidden(symbol)
    }
}
