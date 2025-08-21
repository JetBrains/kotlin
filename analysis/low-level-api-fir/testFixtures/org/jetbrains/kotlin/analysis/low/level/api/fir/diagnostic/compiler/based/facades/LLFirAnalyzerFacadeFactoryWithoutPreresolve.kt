/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.LowLevelFirAnalyzerFacade
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.test.model.TestFile

object LLFirAnalyzerFacadeFactoryWithoutPreresolve : LLFirAnalyzerFacadeFactory() {
    override fun createFirFacade(
        resolutionFacade: LLResolutionFacade,
        allFirFiles: Map<TestFile, FirFile>,
        diagnosticCheckerFilter: DiagnosticCheckerFilter
    ): LowLevelFirAnalyzerFacade {
        return LowLevelFirAnalyzerFacade(resolutionFacade, allFirFiles, diagnosticCheckerFilter)
    }
}
