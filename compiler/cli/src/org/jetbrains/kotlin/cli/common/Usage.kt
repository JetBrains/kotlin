/*
 * Copyright 2010-2026 JetBrains s.r.o.
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
package org.jetbrains.kotlin.cli.common

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.cli.common.arguments.ARGFILE_ARGUMENT
import org.jetbrains.kotlin.cli.common.arguments.ArgumentField
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.getArgumentsInfo
import org.jetbrains.kotlin.cli.common.arguments.isAdvanced
import org.jetbrains.kotlin.cli.common.arguments.isInternal

object Usage {
    const val BAT_DELIMITER_CHARACTERS_NOTE: String =
        "Note: on Windows, arguments that contain delimiter characters (whitespace, =, ;, ,) need to be surrounded with double quotes (\")."

    // The magic number 29 corresponds to the similar padding width in javac and scalac command line compilers
    private const val OPTION_NAME_PADDING_WIDTH = 29
    private val PADDING_STRING = " ".repeat(OPTION_NAME_PADDING_WIDTH)

    fun <A : CommonCompilerArguments> render(compiler: CLICompiler<A>, arguments: A): String {
        val extraHelp = arguments.extraHelp
        return buildString {
            appendLine("Usage: " + compiler.executableScriptFileName() + " <options> <source files>")
            appendLine("where " + (if (extraHelp) "advanced" else "possible") + " options include:")

            // Use distinct because same arguments can be bound to different names (shortName, deprecatedName)
            val argumentFields = getArgumentsInfo(arguments.javaClass).cliArgNameToArguments.values.distinct()
            argumentFields.forEach { argumentField -> fieldUsage(argumentField, extraHelp) }

            if (extraHelp) {
                appendLine()
                appendLine("Advanced options are non-standard and may be changed or removed without any notice.")
            } else {
                renderOptionJUsage()
                renderArgfileUsage()
            }

            if (SystemInfo.isWindows) {
                appendLine()
                appendLine(BAT_DELIMITER_CHARACTERS_NOTE)
            }

            if (!extraHelp) {
                appendLine()
                appendLine("For details, see https://kotl.in/cli")
            }
        }
    }

    context(sb: StringBuilder)
    private fun fieldUsage(argumentField: ArgumentField, extraHelp: Boolean) {
        val argument = argumentField.argument

        if (argument.isInternal || extraHelp != argument.isAdvanced) return

        with(sb) {
            val startLength = length
            append("  ")
            append(argument.value)

            if (argument.shortName.isNotEmpty()) {
                append(" (")
                append(argument.shortName)
                append(')')
            }

            if (argument.valueDescription.isNotEmpty()) {
                append(if (argument.isAdvanced) '=' else ' ')
                append(argument.valueDescription)
            }

            var margin = startLength + OPTION_NAME_PADDING_WIDTH - 1
            if (length >= margin + 5) { // Break the line if it's too long
                appendLine()
                margin += length - startLength
            }
            while (length < margin) {
                append(' ')
            }

            append(' ')

            appendLine(argument.description.replace("\n", "\n" + PADDING_STRING))

            val messageWithStatus = argumentField.generateLifecycleWarning(forExtraHelp = true)
            if (messageWithStatus != null) {
                append(PADDING_STRING)
                append(messageWithStatus.first)
                append('\n')
            }
        }
    }

    context(sb: StringBuilder)
    private fun renderOptionJUsage() {
        with(sb) {
            val descriptionStart = length + OPTION_NAME_PADDING_WIDTH
            append("  -J<option>")
            while (length < descriptionStart) {
                append(" ")
            }
            appendLine("Pass an option directly to JVM.")
        }
    }

    context(sb: StringBuilder)
    private fun renderArgfileUsage() {
        with(sb) {
            val descriptionStart = sb.length + OPTION_NAME_PADDING_WIDTH
            append("  ")
            append(ARGFILE_ARGUMENT)
            append("<argfile>")
            while (length < descriptionStart) {
                append(' ')
            }
            appendLine("Read compiler arguments and file paths from the given file.")
        }
    }
}
