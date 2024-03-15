/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.visitors.FirTransformer

@OptIn(AdapterForResolveProcessor::class)
class FirConstantEvaluationProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.CONSTANT_EVALUATION) {
    override val transformer = FirConstantEvaluationTransformerAdapter(session, scopeSession)
}

@Suppress("UNUSED_PARAMETER")
@AdapterForResolveProcessor
class FirConstantEvaluationTransformerAdapter(session: FirSession, scopeSession: ScopeSession) : FirTransformer<Any?>() {
    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        error("Should only be called via transformFile()")
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        return file
    }
}
