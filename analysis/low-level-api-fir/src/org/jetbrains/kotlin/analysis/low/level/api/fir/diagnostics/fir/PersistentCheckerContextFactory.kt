/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir

import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.LLFirDesignatedImpliciteTypesBodyResolveTransformerForReturnTypeCalculator

internal object PersistentCheckerContextFactory {
    fun createEmptyPersistenceCheckerContext(sessionHolder: SessionHolder): PersistentCheckerContext {
        val returnTypeCalculator = createReturnTypeCalculatorForIDE(
            sessionHolder.scopeSession,
            ImplicitBodyResolveComputationSession(),
            ::LLFirDesignatedImpliciteTypesBodyResolveTransformerForReturnTypeCalculator
        )
        return PersistentCheckerContext(sessionHolder, returnTypeCalculator)
    }
}