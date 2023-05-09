/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.LLFirReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession

internal object PersistentCheckerContextFactory {
    fun createEmptyPersistenceCheckerContext(sessionHolder: SessionHolder): PersistentCheckerContext {
        val returnTypeCalculator = LLFirReturnTypeCalculatorWithJump(
            scopeSession = sessionHolder.scopeSession,
            implicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession(),
            lockProvider = LLFirGlobalResolveComponents.getInstance(sessionHolder.session).lockProvider,
            towerDataContextCollector = null,
        )

        return PersistentCheckerContext(sessionHolder, returnTypeCalculator)
    }
}