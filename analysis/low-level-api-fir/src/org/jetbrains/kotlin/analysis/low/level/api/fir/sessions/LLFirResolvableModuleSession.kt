/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

abstract class LLFirResolvableModuleSession(
    ktModule: KtModule,
    builtinTypes: BuiltinTypes
) : LLFirModuleSession(ktModule, builtinTypes, Kind.Source) {
    internal abstract val moduleComponents: LLFirModuleResolveComponents

    final override fun getScopeSession(): ScopeSession {
        return moduleComponents.scopeSessionProvider.getScopeSession()
    }
}

internal val FirElementWithResolveState.llFirResolvableSession: LLFirResolvableModuleSession?
    get() = llFirSession as? LLFirResolvableModuleSession

internal val FirBasedSymbol<*>.llFirResolvableSession: LLFirResolvableModuleSession?
    get() = fir.llFirResolvableSession