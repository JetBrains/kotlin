/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.uitls

import java.nio.file.Files
import java.nio.file.Path

object IgnoreTests {
    private const val INSERT_DIRECTIVE_AUTOMATICALLY = false // TODO use environment variable instead
    private const val ALWAYS_CONSIDER_TEST_AS_PASSING = false // TODO use environment variable instead

    fun runTestIfEnabledByFileDirective(
        testFile: Path,
        enableTestDirective: String,
        vararg additionalFilesExtensions: String,
        directivePosition: DirectivePosition = DirectivePosition.FIRST_LINE_IN_FILE,
        test: () -> Unit,
    ) {
        runTestIfEnabledByDirective(
            testFile,
            EnableOrDisableTestDirective.Enable(enableTestDirective),
            directivePosition,
            additionalFilesExtensions.toList(),
            test
        )
    }

    fun runTestWithFixMeSupport(
        testFile: Path,
        directivePosition: DirectivePosition = DirectivePosition.FIRST_LINE_IN_FILE,
        test: () -> Unit
    ) {
        runTestIfEnabledByDirective(
            testFile,
            EnableOrDisableTestDirective.Disable(DIRECTIVES.FIX_ME),
            directivePosition,
            additionalFilesExtensions = emptyList(),
            test = test
        )
    }

    fun runTestIfNotDisabledByFileDirective(
        testFile: Path,
        disableTestDirective: String,
        vararg additionalFilesExtensions: String,
        directivePosition: DirectivePosition = DirectivePosition.FIRST_LINE_IN_FILE,
        test: () -> Unit
    ) {
        runTestIfEnabledByDirective(
            testFile,
            EnableOrDisableTestDirective.Disable(disableTestDirective),
            directivePosition,
            additionalFilesExtensions.toList(),
            test
        )
    }

    private fun runTestIfEnabledByDirective(
        testFile: Path,
        directive: EnableOrDisableTestDirective,
        directivePosition: DirectivePosition,
        additionalFilesExtensions: List<String>,
        test: () -> Unit
    ) {
        if (ALWAYS_CONSIDER_TEST_AS_PASSING) {
            test()
            return
        }

        val testIsEnabled = directive.isEnabledInFile(testFile)

        try {
            test()
        } catch (e: Throwable) {
            if (testIsEnabled) {
                if (directive is EnableOrDisableTestDirective.Disable) {
                    handleTestWithWrongDirective(testFile, directive, directivePosition, additionalFilesExtensions)
                }
                throw e
            }
            return
        }

        if (!testIsEnabled) {
            handleTestWithWrongDirective(testFile, directive, directivePosition, additionalFilesExtensions)
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    private fun handleTestWithWrongDirective(
        testFile: Path,
        directive: EnableOrDisableTestDirective,
        directivePosition: DirectivePosition,
        additionalFilesExtensions: List<String>,
    ) {
        val verb = when (directive) {
            is EnableOrDisableTestDirective.Disable -> "do not pass"
            is EnableOrDisableTestDirective.Enable -> "passes"
        }
        if (INSERT_DIRECTIVE_AUTOMATICALLY) {
            testFile.insertDirectivesToFileAndAdditionalFile(directive, additionalFilesExtensions, directivePosition)
            val filesWithDirectiveAdded = buildList {
                add(testFile.fileName.toString())
                additionalFilesExtensions.mapTo(this) { extension -> testFile.getSiblingFile(extension) }
            }
            throw AssertionError(
                "Looks like the test $verb, ${directive.directiveText} was added to the ${filesWithDirectiveAdded.joinToString()}"
            )
        }
        if (directive is EnableOrDisableTestDirective.Enable) {
            throw AssertionError(
                "Looks like the test $verb, please ${directive.fixDirectiveMessage} the ${testFile.fileName}"
            )
        }
    }

    private fun Path.insertDirectivesToFileAndAdditionalFile(
        directive: EnableOrDisableTestDirective,
        additionalFilesExtensions: List<String>,
        directivePosition: DirectivePosition,
    ) {
        insertDirective(directive, directivePosition)
        additionalFilesExtensions.forEach { extension ->
            getSiblingFile(extension)?.insertDirective(directive, directivePosition)
        }
    }

    private fun Path.getSiblingFile(extension: String): Path? {
        val siblingName = fileName.toString() + "." + extension.removePrefix(".")
        return resolveSibling(siblingName).takeIf(Files::exists)
    }

    private sealed class EnableOrDisableTestDirective {
        abstract val directiveText: String
        abstract val fixDirectiveMessage: String

        abstract fun isEnabledIfDirectivePresent(isDirectivePresent: Boolean): Boolean

        data class Enable(override val directiveText: String) : EnableOrDisableTestDirective() {
            override val fixDirectiveMessage: String get() = "add $directiveText to"

            override fun isEnabledIfDirectivePresent(isDirectivePresent: Boolean): Boolean = isDirectivePresent
        }

        data class Disable(override val directiveText: String) : EnableOrDisableTestDirective() {
            override val fixDirectiveMessage: String get() = "remove $directiveText from"
            override fun isEnabledIfDirectivePresent(isDirectivePresent: Boolean): Boolean = !isDirectivePresent
        }
    }

    private fun EnableOrDisableTestDirective.isEnabledInFile(file: Path): Boolean {
        val isDirectivePresent = file.toFile().readText().contains(directiveText)
        return isEnabledIfDirectivePresent(isDirectivePresent)
    }

    private fun Path.insertDirective(directive: EnableOrDisableTestDirective, directivePosition: DirectivePosition) {
        toFile().apply {
            val originalText = readText()
            val textWithDirective = when (directivePosition) {
                DirectivePosition.FIRST_LINE_IN_FILE -> "${directive.directiveText}\n$originalText"
                DirectivePosition.LAST_LINE_IN_FILE -> "$originalText\n${directive.directiveText}"
            }
            writeText(textWithDirective)
        }
    }

    object DIRECTIVES {
        const val FIR_COMPARISON = "// FIR_COMPARISON"
        const val FIR_COMPARISON_MUTLTILINE_COMMENT = "/* FIR_COMPARISON */"
        const val IGNORE_FIR = "// IGNORE_FIR"
        const val FIX_ME = "// FIX_ME: "
    }

    enum class DirectivePosition {
        FIRST_LINE_IN_FILE, LAST_LINE_IN_FILE
    }
}