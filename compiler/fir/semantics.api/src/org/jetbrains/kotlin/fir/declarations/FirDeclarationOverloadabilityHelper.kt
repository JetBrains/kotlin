/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature

interface FirDeclarationOverloadabilityHelper : FirSessionComponent {
    fun isConflicting(a: FirCallableSymbol<*>, b: FirCallableSymbol<*>): Boolean

    enum class ContextParameterShadowing {
        None,
        Shadowing,
        BothWays,
    }

    fun getContextParameterShadowing(a: FirCallableSymbol<*>, b: FirCallableSymbol<*>): ContextParameterShadowing

    fun isExtensionShadowedByMember(extension: FirCallableSymbol<*>, member: FirCallableSymbol<*>): Boolean
}

val FirSession.declarationOverloadabilityHelper: FirDeclarationOverloadabilityHelper by FirSession.sessionComponentAccessor()
