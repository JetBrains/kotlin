/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.createCompilerProcessorByPhase
import java.io.File

abstract class AbstractLazyBodyIsNotTouchedTilContractsPhaseTest : AbstractFirBaseDiagnosticsTest() {
    override val useLazyBodiesModeForRawFir: Boolean get() = true

    override fun runAnalysis(testDataFile: File, testFiles: List<TestFile>, firFilesPerSession: Map<FirSession, List<FirFile>>) {
        val phases = FirResolvePhase.values().filter { phase -> phase != FirResolvePhase.RAW_FIR && phase < FirResolvePhase.CONTRACTS }
        for ((session, firFiles) in firFilesPerSession) {
            val scopeSession = ScopeSession()
            /*
             Test that we are not touching lazy bodies & lazy expressions during phases < CONTRACTS
             If we try to access them, the exception will be thrown and test will fail
             */
            doFirResolveTestBench(
                firFiles,
                phases.map { it.createCompilerProcessorByPhase(session, scopeSession) },
                gc = false
            )
        }
    }
}