/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

interface FirDeclarationOverloadabilityHelper : FirSessionComponent {
    fun isOverloadable(a: FirCallableDeclaration, b: FirCallableDeclaration): Boolean
}

val FirSession.declarationOverloadabilityHelper: FirDeclarationOverloadabilityHelper by FirSession.sessionComponentAccessor()
