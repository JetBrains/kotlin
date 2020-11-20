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

    private val processors: List<FirResolveProcessor> = createAllCompilerResolveProcessors(session, scopeSession)

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

fun createAllCompilerResolveProcessors(
    session: FirSession,
    scopeSession: ScopeSession? = null,
    pluginPhasesEnabled: Boolean = false
): List<FirResolveProcessor> {
    return createAllResolveProcessors(scopeSession, pluginPhasesEnabled) {
        createCompilerProcessorByPhase(session, it)
    }
}

fun createAllTransformerBasedResolveProcessors(
    session: FirSession,
    scopeSession: ScopeSession? = null,
    pluginPhasesEnabled: Boolean = false,
): List<FirTransformerBasedResolveProcessor> {
    return createAllResolveProcessors(scopeSession, pluginPhasesEnabled) {
        createTransformerBasedProcessorByPhase(session, it)
    }
}

private inline fun <T : FirResolveProcessor> createAllResolveProcessors(
    scopeSession: ScopeSession? = null,
    pluginPhasesEnabled: Boolean,
    creator: FirResolvePhase.(ScopeSession) -> T
): List<T> {
    @Suppress("NAME_SHADOWING")
    val scopeSession = scopeSession ?: ScopeSession()
    val phases = FirResolvePhase.values().filter {
        !it.noProcessor && if (!pluginPhasesEnabled) !it.pluginPhase else true
    }
    return phases.map { it.creator(scopeSession) }
}
