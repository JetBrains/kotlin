/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

@OptIn(AdapterForResolveProcessor::class)
class FirAnnotationArgumentsResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer: FirTransformer<Any?> = FirAnnotationArgumentsResolveTransformerAdapter(session, scopeSession)
}

@AdapterForResolveProcessor
class FirAnnotationArgumentsResolveTransformerAdapter(session: FirSession, scopeSession: ScopeSession) : FirTransformer<Any?>() {
    private val transformer = FirAnnotationArgumentsResolveTransformer(session, scopeSession, FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        return file.transform(transformer, ResolutionMode.ContextIndependent)
    }
}
