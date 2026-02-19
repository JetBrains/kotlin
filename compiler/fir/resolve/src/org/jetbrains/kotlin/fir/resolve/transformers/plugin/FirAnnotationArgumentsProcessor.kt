/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.AdapterForResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping

@OptIn(AdapterForResolveProcessor::class)
class FirAnnotationArgumentsProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.ANNOTATION_ARGUMENTS) {
    override val transformer: FirTransformer<Any?> = FirAnnotationArgumentsTransformerAdapter(session, scopeSession)
}

@AdapterForResolveProcessor
class FirAnnotationArgumentsTransformerAdapter(session: FirSession, scopeSession: ScopeSession) : FirTransformer<Any?>() {
    private val transformer = FirAnnotationArgumentsTransformer(session, scopeSession, FirResolvePhase.ANNOTATION_ARGUMENTS)

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        error("Should only be called via transformFile()")
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        return withFileAnalysisExceptionWrapping(file) {
            file.transform(transformer, ResolutionMode.ContextIndependent)
        }
    }
}
