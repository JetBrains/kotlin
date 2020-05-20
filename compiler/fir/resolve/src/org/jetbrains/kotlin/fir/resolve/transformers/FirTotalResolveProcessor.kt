/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession

class FirTotalResolveProcessor(session: FirSession) {
    val scopeSession: ScopeSession = ScopeSession()

    private val processors: List<FirResolveProcessor> = createAllResolveProcessors(session, scopeSession)

    fun process(files: List<FirFile>) {
        for (processor in processors) {
            when (processor) {
                is FirTransformerBasedResolveProcessor -> {
                    for (file in files) {
                        processor.processFile(file)
                    }
                }
                is FirGlobalResolveProcessor -> {
                    processor.process()
                }
            }
        }
    }
}

fun createAllResolveProcessors(
    session: FirSession,
    scopeSession: ScopeSession? = null,
    mode: CompilerMode = CompilerMode.CLI
): List<FirResolveProcessor> {
    @Suppress("NAME_SHADOWING")
    val scopeSession = scopeSession ?: ScopeSession()
    return FirResolvePhase.values()
        .drop(1) // to remove RAW_FIR phase
        .map { it.createProcessorByPhase(session, scopeSession, mode) }
}

fun createAllTransformerBasedResolveProcessors(
    session: FirSession,
    scopeSession: ScopeSession? = null,
): List<FirTransformerBasedResolveProcessor> {
    @Suppress("UNCHECKED_CAST")
    return createAllResolveProcessors(session, scopeSession, CompilerMode.IDE) as List<FirTransformerBasedResolveProcessor>
}