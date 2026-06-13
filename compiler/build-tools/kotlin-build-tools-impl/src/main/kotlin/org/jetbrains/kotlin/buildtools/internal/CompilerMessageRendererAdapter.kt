/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.render
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer

internal interface MessageRendererWithDiagnosticId : MessageRenderer {
    fun render(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
        diagnosticId: String?,
    ): String
}

internal class CompilerMessageRendererAdapter(
    private val compilerMessageRenderer: CompilerMessageRenderer,
) : MessageRendererWithDiagnosticId {

    override fun render(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ): String {
        return render(severity, message, location, diagnosticId = null)
    }

    override fun render(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
        diagnosticId: String?,
    ): String {
        return compilerMessageRenderer.render(
            severity.asCompilerMessageRendererSeverity(),
            message,
            location?.asCompilerMessageRendererSourceLocation(),
            diagnosticId
        )
    }

    override fun renderPreamble() = ""

    override fun renderUsage(usage: String) = usage

    override fun renderConclusion() = ""

    override fun getName() = "BuildToolsApi"

    private fun CompilerMessageSeverity.asCompilerMessageRendererSeverity(): CompilerMessageRenderer.Severity =
        when (this) {
            CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING, CompilerMessageSeverity.FIXED_WARNING -> {
                CompilerMessageRenderer.Severity.WARNING
            }
            CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> {
                CompilerMessageRenderer.Severity.ERROR
            }
            CompilerMessageSeverity.LOGGING, CompilerMessageSeverity.OUTPUT -> {
                CompilerMessageRenderer.Severity.DEBUG
            }
            CompilerMessageSeverity.INFO -> {
                CompilerMessageRenderer.Severity.INFO
            }
        }

    private fun CompilerMessageSourceLocation.asCompilerMessageRendererSourceLocation() =
        CompilerMessageRenderer.SourceLocation(
            path = path,
            line = line,
            column = column,
            lineEnd = lineEnd,
            columnEnd = columnEnd,
            lineContent = lineContent,
        )
}

internal fun CompilerMessageRenderer.asMessageRenderer(): MessageRenderer = CompilerMessageRendererAdapter(this)

internal fun MessageRenderer.render(
    severity: CompilerMessageSeverity,
    message: String,
    location: CompilerMessageSourceLocation?,
    diagnosticId: String?,
): String {
    return when (this) {
        is MessageRendererWithDiagnosticId -> render(severity, message, location, diagnosticId)
        else -> render(severity, message, location)
    }
}
