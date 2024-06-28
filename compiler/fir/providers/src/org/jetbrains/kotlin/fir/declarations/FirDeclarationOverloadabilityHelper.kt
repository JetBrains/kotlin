/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

interface FirDeclarationOverloadabilityHelper : FirSessionComponent {
    fun isOverloadable(a: FirCallableSymbol<*>, b: FirCallableSymbol<*>): Boolean
}

val FirSession.declarationOverloadabilityHelper: FirDeclarationOverloadabilityHelper by FirSession.sessionComponentAccessor()
