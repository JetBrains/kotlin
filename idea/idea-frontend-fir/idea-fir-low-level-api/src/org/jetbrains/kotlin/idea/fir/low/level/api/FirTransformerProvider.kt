/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.google.common.collect.MapMaker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.createTransformerBasedProcessorByPhase
import java.util.*

class FirTransformerProvider {
    private val scopeSession = MapMaker().weakKeys().makeMap<FirSession, ScopeSession>()
    private val transformers = MapMaker().weakKeys().makeMap<FirSession, EnumMap<FirResolvePhase, FirTransformerBasedResolveProcessor>>()


    fun getTransformerForPhase(session: FirSession, phase: FirResolvePhase): FirTransformerBasedResolveProcessor =
        transformers.computeIfAbsent(session) {
            val scopesSession = getScopeSession(session)
            EnumMap<FirResolvePhase, FirTransformerBasedResolveProcessor>(FirResolvePhase::class.java).apply {
                FirResolvePhase.values().forEach { resolvePhase ->
                    if (resolvePhase != FirResolvePhase.RAW_FIR) {
                        put(resolvePhase, resolvePhase.createTransformerBasedProcessorByPhase(session, scopesSession))
                    }
                }
            }
        }.getValue(phase)

    fun getScopeSession(session: FirSession): ScopeSession =
        scopeSession.getOrPut(session) { ScopeSession() }
}
