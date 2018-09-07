/*
* Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.cli.common.arguments

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Reader

const val ARGFILE_ARGUMENT = "@"
private const val EXPERIMENTAL_ARGFILE_ARGUMENT = "-Xargfile="

private const val SINGLE_QUOTE = '\''
private const val DOUBLE_QUOTE = '"'
private const val BACKSLASH = '\\'
private const val WHITESPACE = ' '
private const val NEWLINE = '\n'

/**
 * Performs initial preprocessing of arguments, passed to the compiler.
 * This is done prior to *any* arguments parsing, and result of preprocessing
 * will be used instead of actual passed arguments.
 */
internal fun preprocessCommandLineArguments(args: List<String>, errors: ArgumentParseErrors): List<String> =
    args.flatMap { arg ->
        if (arg.isArgfileArgument) {
            File(arg.argfilePath).expand(errors)
        } else if (arg.isDeprecatedArgfileArgument) {
            errors.deprecatedArguments[EXPERIMENTAL_ARGFILE_ARGUMENT] = ARGFILE_ARGUMENT

            File(arg.deprecatedArgfilePath).expand(errors)
        } else {
            listOf(arg)
        }
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

    var r = nextChar()
    while (r != null && (r == WHITESPACE || r == NEWLINE)) {
        r = nextChar()
    }

    loop@ while (r != null) {
        when (r) {
            WHITESPACE, NEWLINE -> break@loop
            DOUBLE_QUOTE, SINGLE_QUOTE -> consumeRestOfEscapedSequence(sb, r)
            BACKSLASH -> nextChar()?.apply(sb::append)
            else -> sb.append(r)
        }

        r = nextChar()
    }

    return sb.toString().takeIf { it.isNotEmpty() }
}

private fun Reader.consumeRestOfEscapedSequence(sb: StringBuilder, quote: Char) {
    var ch = nextChar()
    while (ch != null && ch != quote) {
        if (ch == BACKSLASH) nextChar()?.apply(sb::append) else sb.append(ch)
        ch = nextChar()
    }
}

private fun Reader.nextChar(): Char? =
    read().takeUnless { it == -1 }?.toChar()

private val String.argfilePath: String
    get() = removePrefix(ARGFILE_ARGUMENT)

private val String.isArgfileArgument: Boolean
    get() = startsWith(ARGFILE_ARGUMENT)

private val String.deprecatedArgfilePath: String
    get() = removePrefix(EXPERIMENTAL_ARGFILE_ARGUMENT)

private val String.isDeprecatedArgfileArgument: Boolean
    get() = startsWith(EXPERIMENTAL_ARGFILE_ARGUMENT)
