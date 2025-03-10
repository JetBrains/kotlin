/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import java.io.File

internal object KotlinMessageRenderer : MessageRenderer {
    override fun render(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?): String {
        return buildString {
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

    override fun getName() = "BuildToolsApi"
}