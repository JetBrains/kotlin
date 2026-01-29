/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.pipeline

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.pipeline.Fir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.FrontendPipelineArtifact
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult

class JKlibFrontendPipelineArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    override val diagnosticCollector: BaseDiagnosticsCollector,
    val sourceFiles: List<KtSourceFile>,
    val projectEnvironment: VfsBasedProjectEnvironment,
    val rootDisposable: Disposable
) : FrontendPipelineArtifact() {
    override fun withNewDiagnosticCollectorImpl(newDiagnosticsCollector: BaseDiagnosticsCollector): FrontendPipelineArtifact {
        return JKlibFrontendPipelineArtifact(frontendOutput, configuration, newDiagnosticsCollector, sourceFiles, projectEnvironment, rootDisposable)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return JKlibFrontendPipelineArtifact(newFrontendOutput, configuration, diagnosticCollector, sourceFiles, projectEnvironment, rootDisposable)
    }
}

class JKlibFir2IrPipelineArtifact(
    override val result: Fir2IrActualizedResult,
    override val diagnosticCollector: BaseDiagnosticsCollector,
    val configuration: CompilerConfiguration,
    val frontendOutput: AllModulesFrontendOutput,
    val projectEnvironment: VfsBasedProjectEnvironment, // Passed down for extensions or cleanup
    val rootDisposable: Disposable
) : Fir2IrPipelineArtifact()
