/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer

internal class CompilerMessageRendererAdapter(private val compilerMessageRenderer: CompilerMessageRenderer) : MessageRenderer {

    override fun render(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ): String? {
        return compilerMessageRenderer.render(
            severity.asCompilerMessageRendererSeverity(),
            message,
            location?.asCompilerMessageRendererSourceLocation()
        )
    }

    override fun renderPreamble() = ""

    override fun renderUsage(usage: String) = usage

    override fun renderConclusion() = ""

    override fun getName() = "BuildToolsApi"

    private fun CompilerMessageSeverity.asCompilerMessageRendererSeverity(): CompilerMessageRenderer.Severity =
        when (this) {
            CompilerMessageSeverity.INFO -> CompilerMessageRenderer.Severity.INFO
            CompilerMessageSeverity.WARNING -> CompilerMessageRenderer.Severity.WARNING
            CompilerMessageSeverity.ERROR -> CompilerMessageRenderer.Severity.ERROR
            CompilerMessageSeverity.STRONG_WARNING -> CompilerMessageRenderer.Severity.STRONG_WARNING
            CompilerMessageSeverity.EXCEPTION -> CompilerMessageRenderer.Severity.EXCEPTION
            CompilerMessageSeverity.FIXED_WARNING -> CompilerMessageRenderer.Severity.FIXED_WARNING
            CompilerMessageSeverity.LOGGING -> CompilerMessageRenderer.Severity.LOGGING
            CompilerMessageSeverity.OUTPUT -> CompilerMessageRenderer.Severity.OUTPUT
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
