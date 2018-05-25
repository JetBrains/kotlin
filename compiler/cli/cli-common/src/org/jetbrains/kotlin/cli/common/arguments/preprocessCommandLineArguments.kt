/*
* Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.cli.common.arguments

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Reader

private const val EXPERIMENTAL_ARGFILE_ARGUMENT = "-Xargfile"

private const val QUOTATION_MARK = '"'
private const val BACKSLASH = '\\'
private const val WHITESPACE = ' '
private const val NEWLINE = '\n'

/**
 * Performs initial preprocessing of arguments, passed to the compiler.
 * This is done prior to *any* arguments parsing, and result of preprocessing
 * will be used instead of actual passed arguments.
 */
internal fun preprocessCommandLineArguments(args: List<String>, errors: ArgumentParseErrors): List<String> =
    args.flatMap {
        if (it.isArgumentForArgfile)
            File(it.argfilePath).expand(errors)
        else
            listOf(it)
    }

private fun File.expand(errors: ArgumentParseErrors): List<String> {
    return try {
        bufferedReader(Charsets.UTF_8).use {
            generateSequence { it.parseNextArgument() }.toList()
        }
    } catch (e: FileNotFoundException) {
        // Process FNFE separately to render absolutePath in error message
        errors.argfileErrors += "Argfile not found: $absolutePath"
        emptyList()
    } catch (e: IOException) {
        errors.argfileErrors += "Error while reading argfile: $e"
        emptyList()
    }
}

private fun Reader.parseNextArgument(): String? {
    val sb = StringBuilder()

    var r: Int = read()
    while (r != -1) {
        when (r.toChar()) {
            WHITESPACE, NEWLINE -> return sb.toString()
            QUOTATION_MARK -> consumeRestOfEscapedSequence(sb)
            BACKSLASH -> sb.append(read().toChar())
            else -> sb.append(r.toChar())
        }

        r = read()
    }

    return sb.toString().takeIf { it.isNotEmpty() }
}

private fun Reader.consumeRestOfEscapedSequence(sb: StringBuilder) {
    var ch = read().toChar()
    while (ch != QUOTATION_MARK) {
        if (ch == BACKSLASH) sb.append(read().toChar()) else sb.append(ch)
        ch = read().toChar()
    }
}

private val String.argfilePath: String
    get() = removePrefix("$EXPERIMENTAL_ARGFILE_ARGUMENT=")

// Note that currently we use only experimental syntax for passing argfiles
// In 1.3 we can support also javac-like syntax `@argfile`
private val String.isArgumentForArgfile: Boolean
    get() = startsWith("$EXPERIMENTAL_ARGFILE_ARGUMENT=")
