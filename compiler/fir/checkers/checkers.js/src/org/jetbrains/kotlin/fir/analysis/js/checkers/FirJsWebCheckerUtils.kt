/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.FirAbstractWebCheckerUtils
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

object FirJsWebCheckerUtils : FirAbstractWebCheckerUtils() {
    override fun isNativeOrExternalInterface(symbol: FirBasedSymbol<*>, session: FirSession): Boolean {
        return symbol.isNativeInterface(session)
    }
}