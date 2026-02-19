/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.cli.common.messages

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import org.fusesource.jansi.internal.CLibrary
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import java.util.*

// This constructor can be used in a compilation server to still be able to generate colored output, even if stderr is not a TTY.
abstract class PlainTextMessageRenderer @JvmOverloads constructor(private val colorEnabled: Boolean = COLOR_ENABLED) : MessageRenderer {
    override fun renderPreamble(): String {
        return ""
    }

    override fun render(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?): String {
        val result = StringBuilder()

        val line = location?.line ?: -1
        val column = location?.column ?: -1
        val lineEnd = location?.lineEnd ?: -1
        val columnEnd = location?.columnEnd ?: -1
        val lineContent = location?.lineContent

        val path = if (location != null) getPath(location) else null
        if (path != null) {
            result.append(path)
            result.append(":")
            if (line > 0) {
                result.append(line).append(":")
                if (column > 0) {
                    result.append(column).append(":")
                }
            }
            result.append(" ")
        }

        if (this.colorEnabled) {
            val ansi: Ansi = Ansi.ansi()
                .bold()
                .fg(severityColor(severity))
                .a(severity.presentableName)
                .a(": ")
                .reset()

            if (IMPORTANT_MESSAGE_SEVERITIES.contains(severity)) {
                ansi.bold()
            }

            // Only make the first line of the message bold. Otherwise long overload ambiguity errors or exceptions are hard to read
            val decapitalized: String = decapitalizeIfNeeded(message)
            val firstNewline: Int = decapitalized.indexOf(LINE_SEPARATOR)
            if (firstNewline < 0) {
                result.append(ansi.a(decapitalized).reset())
            } else {
                result.append(ansi.a(decapitalized.substring(0, firstNewline)).reset().a(decapitalized.substring(firstNewline)))
            }
        } else {
            result.append(severity.presentableName)
            result.append(": ")
            result.append(decapitalizeIfNeeded(message))
        }

        if (lineContent != null && 1 <= column && column <= lineContent.length + 1) {
            result.append(LINE_SEPARATOR)
            result.append(lineContent)
            result.append(LINE_SEPARATOR)
            result.append(" ".repeat(column - 1))
            if (lineEnd > line) {
                result.append("^".repeat(lineContent.length - column + 1))
            } else if (lineEnd == line && columnEnd > column) {
                result.append("^".repeat(columnEnd - column))
            } else {
                result.append("^")
            }
        }

        return result.toString()
    }

    protected abstract fun getPath(location: CompilerMessageSourceLocation): String?

    override fun renderUsage(usage: String): String {
        return usage
    }

    override fun renderConclusion(): String {
        return ""
    }

    fun enableColorsIfNeeded() {
        if (colorEnabled) {
            AnsiConsole.systemInstall()
        }
    }

    fun disableColorsIfNeeded() {
        if (colorEnabled) {
            AnsiConsole.systemUninstall()
        }
    }

    companion object {
        private val COLOR_ENABLED: Boolean

        init {
            var isStderrATty = false
            // TODO: investigate why ANSI escape codes on Windows only work in REPL for some reason
            val kotlinColorsEnabled = CompilerSystemProperties.KOTLIN_COLORS_ENABLED_PROPERTY.value
            if (!isWindows && "true" == kotlinColorsEnabled) {
                try {
                    isStderrATty = CLibrary.isatty(CLibrary.STDERR_FILENO) != 0
                } catch (_: UnsatisfiedLinkError) {
                }
            }
            COLOR_ENABLED = isStderrATty || "always" == kotlinColorsEnabled
        }

        private val LINE_SEPARATOR: String = System.lineSeparator()

        private val IMPORTANT_MESSAGE_SEVERITIES: Set<CompilerMessageSeverity> = EnumSet.of(
            CompilerMessageSeverity.EXCEPTION,
            CompilerMessageSeverity.ERROR,
            CompilerMessageSeverity.STRONG_WARNING,
            CompilerMessageSeverity.WARNING,
            CompilerMessageSeverity.FIXED_WARNING,
        )

        private fun decapitalizeIfNeeded(message: String): String {
            // TODO: invent something more clever
            // An ad-hoc heuristic to prevent decapitalization of some names
            if (message.startsWith("Java") || message.startsWith("Kotlin")) {
                return message
            }

            // For abbreviations and capitalized text
            if (message.length >= 2 && Character.isUpperCase(message[0]) && Character.isUpperCase(message[1])) {
                return message
            }

            return message.decapitalizeAsciiOnly()
        }

        private fun severityColor(severity: CompilerMessageSeverity): Ansi.Color {
            return when (severity) {
                CompilerMessageSeverity.EXCEPTION,
                CompilerMessageSeverity.ERROR
                    -> Ansi.Color.RED

                CompilerMessageSeverity.STRONG_WARNING,
                CompilerMessageSeverity.WARNING,
                CompilerMessageSeverity.FIXED_WARNING
                    -> Ansi.Color.YELLOW

                CompilerMessageSeverity.INFO,
                CompilerMessageSeverity.LOGGING,
                CompilerMessageSeverity.OUTPUT
                    -> Ansi.Color.BLUE
            }
        }
    }
}
