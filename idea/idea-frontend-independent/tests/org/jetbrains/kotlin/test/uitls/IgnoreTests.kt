/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.uitls

import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.nio.file.Files
import java.nio.file.Path

object IgnoreTests {
    private const val INSERT_DIRECTIVE_AUTOMATICALLY = false // TODO use environment variable instead

    fun runTestIfEnabledByFileDirective(
        testFile: Path,
        enableTestDirective: String,
        vararg additionalFilesExtensions: String,
        test: () -> Unit,
    ) {
        runTestIfEnabledByDirective(
            testFile,
            EnableOrDisableTestDirective.Enable(enableTestDirective),
            additionalFilesExtensions.toList(),
            test
        )
    }

    fun runTestIfNotDisabledByFileDirective(
        testFile: Path,
        disableTestDirective: String,
        vararg additionalFilesExtensions: String,
        test: () -> Unit
    ) {
        runTestIfEnabledByDirective(
            testFile,
            EnableOrDisableTestDirective.Disable(disableTestDirective),
            additionalFilesExtensions.toList(),
            test
        )
    }

    private fun runTestIfEnabledByDirective(
        testFile: Path,
        directive: EnableOrDisableTestDirective,
        additionalFilesExtensions: List<String>,
        test: () -> Unit
    ) {
        val testIsEnabled = directive.isEnabledInFile(testFile)

        try {
            test()
        } catch (e: Throwable) {
            if (testIsEnabled) throw e
            return
        }

        if (!testIsEnabled) {
            handlePassingButNotEnabledTest(testFile, directive, additionalFilesExtensions)
        }
    }

    private fun handlePassingButNotEnabledTest(
        testFile: Path,
        directive: EnableOrDisableTestDirective,
        additionalFilesExtensions: List<String>,
    ) {
        if (INSERT_DIRECTIVE_AUTOMATICALLY) {
            testFile.insertDirectivesToFileAndAdditionalFile(directive, additionalFilesExtensions)
        }

        throw AssertionError(
            "Looks like the test passes, please ${directive.fixDirectiveMessage} the beginning of the testdata file"
        )
    }

    private fun Path.insertDirectivesToFileAndAdditionalFile(
        directive: EnableOrDisableTestDirective,
        additionalFilesExtensions: List<String>,
    ) {
        insertDirective(directive)
        additionalFilesExtensions.forEach { extension ->
            getSiblingFile(extension)?.insertDirective(directive)
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
        val isDirectivePresent = InTextDirectivesUtils.isDirectiveDefined(file.toFile().readText(), directiveText)
        return isEnabledIfDirectivePresent(isDirectivePresent)
    }

    private fun Path.insertDirective(directive: EnableOrDisableTestDirective) {
        toFile().apply {
            val originalText = readText()
            writeText("${directive.directiveText}\n$originalText")
        }
    }

    object DIRECTIVES {
        const val FIR_COMPARISON = "// FIR_COMPARISON"
        const val IGNORE_FIR = "// IGNORE_FIR"
    }
}