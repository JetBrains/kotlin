/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

class GradleStyleMessageRenderer : MessageRenderer {

    override fun render(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?): String {
        val prefix = when (severity) {
            CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> "w"
            CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> "e"
            CompilerMessageSeverity.LOGGING, CompilerMessageSeverity.OUTPUT -> "v"
            CompilerMessageSeverity.INFO -> "i"
        }

        return buildString {
            append("$prefix: ")

            location?.apply {
                append("$path: ")
                if (line > 0 && column > 0) {
                    append("($line, $column): ")
                }
            }

            append(message)
        }
    }

    override fun renderPreamble() = ""

    override fun renderUsage(usage: String) = usage

    override fun renderConclusion() = ""

    override fun getName() = "GradleStyle"
}