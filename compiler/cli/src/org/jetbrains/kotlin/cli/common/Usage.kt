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
        val sb = StringBuilder()
        appendln(sb, "Usage: " + compiler.executableScriptFileName() + " <options> <source files>")
        appendln(sb, "where " + (if (extraHelp) "advanced" else "possible") + " options include:")

        // Use distinct because same arguments can be bound to different names (shortName, deprecatedName)
        val argumentFields = getArgumentsInfo(arguments.javaClass).cliArgNameToArguments.values.distinct()
        argumentFields.forEach { argumentField -> fieldUsage(sb, argumentField, extraHelp) }

        if (extraHelp) {
            appendln(sb, "")
            appendln(sb, "Advanced options are non-standard and may be changed or removed without any notice.")
        } else {
            renderOptionJUsage(sb)
            renderArgfileUsage(sb)
        }

        if (SystemInfo.isWindows) {
            appendln(sb, "")
            appendln(sb, BAT_DELIMITER_CHARACTERS_NOTE)
        }

        if (!extraHelp) {
            appendln(sb, "")
            appendln(sb, "For details, see https://kotl.in/cli")
        }

        return sb.toString()
    }

    private fun fieldUsage(sb: StringBuilder, argumentField: ArgumentField, extraHelp: Boolean) {
        val argument = argumentField.argument

        if (argument.isObsolete) return
        if (argument.isInternal) return
        if (extraHelp != argument.isAdvanced) return

        val startLength = sb.length
        sb.append("  ")
        sb.append(argument.value)

        if (!argument.shortName.isEmpty()) {
            sb.append(" (")
            sb.append(argument.shortName)
            sb.append(")")
        }

        if (!argument.valueDescription.isEmpty()) {
            sb.append(if (argument.isAdvanced) "=" else " ")
            sb.append(argument.valueDescription)
        }

        var margin = startLength + OPTION_NAME_PADDING_WIDTH - 1
        if (sb.length >= margin + 5) { // Break the line if it's too long
            sb.append("\n")
            margin += sb.length - startLength
        }
        while (sb.length < margin) {
            sb.append(" ")
        }

        sb.append(" ")

        appendln(sb, argument.description.replace("\n", "\n" + PADDING_STRING))

        val deprecatedAnnotation = argumentField.deprecatedAnnotation
        if (deprecatedAnnotation != null) {
            sb.append(PADDING_STRING)
            sb.append("The option is ")
            // The value is generated automatically based on KotlinReleaseVersion entries, thus it's expected to be always valid.
            val argDeprecatedVersion = parseKotlinVersion(argument.deprecatedVersion)
            val isAlreadyDeprecated = argDeprecatedVersion <= KotlinVersion.CURRENT
            sb.append(if (isAlreadyDeprecated) "deprecated since " else "will be deprecated in ")
            sb.append("Kotlin ").append(argDeprecatedVersion).append('.')
            if (isAlreadyDeprecated) {
                sb.append(" It will be removed in one of the future releases.")
            }
            val message = deprecatedAnnotation.message
            if (!message.isEmpty()) {
                sb.append(' ').append(message.replace("\n", "\n" + PADDING_STRING))
            }
            sb.append('\n')
        }
    }

    private fun renderOptionJUsage(sb: StringBuilder) {
        val descriptionStart = sb.length + OPTION_NAME_PADDING_WIDTH
        sb.append("  -J<option>")
        while (sb.length < descriptionStart) {
            sb.append(" ")
        }
        appendln(sb, "Pass an option directly to JVM.")
    }

    private fun renderArgfileUsage(sb: StringBuilder) {
        val descriptionStart = sb.length + OPTION_NAME_PADDING_WIDTH
        sb.append("  ")
        sb.append(ARGFILE_ARGUMENT)
        sb.append("<argfile>")
        while (sb.length < descriptionStart) {
            sb.append(" ")
        }
        appendln(sb, "Read compiler arguments and file paths from the given file.")
    }

    private fun appendln(sb: StringBuilder, string: String) {
        sb.append(string)
        sb.append('\n')
    }
}
