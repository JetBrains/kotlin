/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.toleratedErrorsTracker
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.backend.ERRONEOUS_CODE_PLAN
import org.jetbrains.kotlin.fir.backend.FirErroneousCodePlan
import org.jetbrains.kotlin.fir.backend.FirErrorTolerantCompilationExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput

/**
 * If there are registered [FirErrorTolerantCompilationExtension]s and the frontend reported errors, moves these errors out
 * of the diagnostics collector (so they don't stop the pipeline), reports them as warnings, and returns a copy of [configuration]
 * with the [ERRONEOUS_CODE_PLAN] computed by the extensions.
 */
internal fun tolerateFrontendErrorsIfNeeded(
    configuration: CompilerConfiguration,
    frontendOutput: AllModulesFrontendOutput,
): CompilerConfiguration {
    val extensions = configuration.getCompilerExtensions(FirErrorTolerantCompilationExtension)
    if (extensions.isEmpty()) return configuration
    val diagnosticsCollector = configuration.diagnosticsCollector as? DiagnosticsCollectorImpl ?: return configuration
    if (!diagnosticsCollector.hasErrors) return configuration

    val errors = diagnosticsCollector.extractErrors()
    reportToleratedErrors(errors, configuration)

    val files = frontendOutput.outputs.flatMap { it.fir }
    val plans = extensions.map { it.computeErroneousCodePlan(files, errors.diagnosticsByFile) }
    return configuration.copy().apply {
        put(ERRONEOUS_CODE_PLAN, plans.singleOrNull() ?: CompositeErroneousCodePlan(plans))
    }
}

@OptIn(MessageCollectorAccess::class)
private fun reportToleratedErrors(errors: BaseDiagnosticsCollector, configuration: CompilerConfiguration) {
    val messageCollector = configuration.messageCollector
    val renderDiagnosticName = configuration.renderDiagnosticInternalName
    val toleratedErrorsTracker = configuration.toleratedErrorsTracker
    FirDiagnosticsCompilerResultsReporter.reportByFile(errors) { diagnostic, location ->
        val diagnosticId = diagnostic.factory.name
        val message = diagnostic.renderMessage()
        val text = if (renderDiagnosticName) "[$diagnosticId] $message" else message
        messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, "$TOLERATED_ERROR_PREFIX$text", location)
        location?.path?.let { toleratedErrorsTracker?.report(it) }
    }
}

const val TOLERATED_ERROR_PREFIX: String = "Tolerated error: "

private class CompositeErroneousCodePlan(private val plans: List<FirErroneousCodePlan>) : FirErroneousCodePlan {
    override fun getStubMessage(declaration: FirDeclaration): String? =
        plans.firstNotNullOfOrNull { it.getStubMessage(declaration) }
}
