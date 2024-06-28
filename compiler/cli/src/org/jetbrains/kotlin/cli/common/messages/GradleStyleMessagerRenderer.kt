/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

import java.io.File

class GradleStyleMessageRenderer : MessageRenderer {

    override fun render(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?): String {
        val prefix = when (severity) {
            CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> "w"
            CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> "e"
            CompilerMessageSeverity.LOGGING, CompilerMessageSeverity.OUTPUT -> "v"
            CompilerMessageSeverity.INFO -> "i"
        }

        return buildString {
            append("$prefix: ")

            location?.apply {
                val fileUri = File(path).toPath().toUri()
                append("$fileUri")
                if (line > 0 && column > 0) {
                    append(":$line:$column")
                }
                append(' ')
            }

            append(message)
        }
    }

    override fun renderPreamble() = ""

    override fun renderUsage(usage: String) = usage

    override fun renderConclusion() = ""

    override fun getName() = "GradleStyle"
}