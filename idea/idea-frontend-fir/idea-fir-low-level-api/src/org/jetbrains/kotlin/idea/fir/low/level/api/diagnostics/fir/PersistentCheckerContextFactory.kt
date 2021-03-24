/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir

import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator

internal object PersistentCheckerContextFactory {
    fun createEmptyPersistenceCheckerContext(sessionHolder: SessionHolder): PersistentCheckerContext {
        val returnTypeCalculator = createReturnTypeCalculatorForIDE(
            sessionHolder.session,
            ScopeSession(),
            ImplicitBodyResolveComputationSession(),
            ::FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
        )
        return PersistentCheckerContext(sessionHolder, returnTypeCalculator)
    }
}