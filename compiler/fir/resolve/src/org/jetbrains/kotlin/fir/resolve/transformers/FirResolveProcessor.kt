/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.visitors.FirTransformer

@RequiresOptIn(message = "Should be used just only in resolve processor")
annotation class AdapterForResolveProcessor

sealed class FirResolveProcessor(val session: FirSession, val scopeSession: ScopeSession) {
    open fun beforePhase() {}

    open fun afterPhase() {}
}

abstract class FirGlobalResolveProcessor(session: FirSession, scopeSession: ScopeSession) : FirResolveProcessor(session, scopeSession) {
    abstract fun process(files: Collection<FirFile>)
}

abstract class FirTransformerBasedResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirResolveProcessor(session, scopeSession) {
    abstract val transformer: FirTransformer<Nothing?>

    open fun processFile(file: FirFile) {
        file.transform<FirFile, Nothing?>(transformer, null)
    }
}
