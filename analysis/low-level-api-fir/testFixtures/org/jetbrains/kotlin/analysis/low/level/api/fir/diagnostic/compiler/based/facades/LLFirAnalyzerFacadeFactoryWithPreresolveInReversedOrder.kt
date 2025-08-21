/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.LowLevelFirAnalyzerFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.getDeclarationsToResolve
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

object LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder : LLFirAnalyzerFacadeFactory() {
    override fun createFirFacade(
        resolutionFacade: LLResolutionFacade,
        allFirFiles: Map<TestFile, FirFile>,
        diagnosticCheckerFilter: DiagnosticCheckerFilter
    ): LowLevelFirAnalyzerFacade = object : LowLevelFirAnalyzerFacade(resolutionFacade, allFirFiles, diagnosticCheckerFilter) {
        override fun runResolution(): List<FirFile> {
            val allDeclarations = allFirFiles.values.getDeclarationsToResolve().reversed()

            // Execution on another thread is needed to avoid thread local caches
            executeOnPooledThreadInReadAction {
                for (declaration in allDeclarations) {
                    declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    declaration.checkPhase(FirResolvePhase.BODY_RESOLVE)
                }
            }

            return allFirFiles.values.toList()
        }
    }
}
