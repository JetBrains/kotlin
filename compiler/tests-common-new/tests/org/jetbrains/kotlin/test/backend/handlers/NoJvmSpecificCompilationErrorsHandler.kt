/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.TestsCompiletimeError
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class NoJvmSpecificCompilationErrorsHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val generationState = info.classFileFactory.generationState
        try {
            AnalyzingUtils.throwExceptionOnErrors(generationState.collectedExtraJvmDiagnostics)
            FirDiagnosticsCompilerResultsReporter.throwFirstErrorAsException(generationState.diagnosticReporter as BaseDiagnosticsCollector)
        } catch (e: Throwable) {
            throw TestsCompiletimeError(e)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
