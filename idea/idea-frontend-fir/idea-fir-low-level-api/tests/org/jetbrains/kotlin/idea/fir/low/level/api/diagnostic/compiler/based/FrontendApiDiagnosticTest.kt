/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostic.compiler.based

import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.FrontendApiTestWithTestdata
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.LowLevelFirAnalyzerFacade
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.LowLevelFirOutputArtifact
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.getKtFilesForSourceFiles
import org.jetbrains.kotlin.test.services.sourceFileProvider

abstract class FrontendApiDiagnosticTest : FrontendApiTestWithTestdata() {

    override fun getArtifact(
        module: TestModule,
        testServices: TestServices,
        ktFiles: Map<TestFile, KtFile>,
        resolveState: FirModuleResolveState
    ): LowLevelFirOutputArtifact {
        val allFirFiles = ktFiles.map { (testFile, psiFile) ->
            testFile to psiFile.getOrBuildFirFile(resolveState)
        }.toMap()

        val diagnosticCheckerFilter = if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
            DiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS
        } else DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS

        val analyzerFacade = LowLevelFirAnalyzerFacade(resolveState, allFirFiles, diagnosticCheckerFilter)
        return LowLevelFirOutputArtifact(resolveState.rootModuleSession, analyzerFacade)
    }
}