/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KtAssert
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
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
            computeAdditionalFilesByExtensions(testFile, additionalFilesExtensions.asList()),
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
            additionalFiles = emptyList(),
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
        runTestIfNotDisabledByFileDirective(
            testFile,
            disableTestDirective,
            { mainTestFile -> computeAdditionalFilesByExtensions(mainTestFile, additionalFilesExtensions.asList()) },
            directivePosition,
            test
        )
    }

    fun runTestIfNotDisabledByFileDirective(
        testFile: Path,
        disableTestDirective: String,
        computeAdditionalFiles: (mainTestFile: Path) -> List<Path>,
        directivePosition: DirectivePosition = DirectivePosition.FIRST_LINE_IN_FILE,
        test: () -> Unit
    ) {
        runTestIfEnabledByDirective(
            testFile,
            EnableOrDisableTestDirective.Disable(disableTestDirective),
            directivePosition,
            computeAdditionalFiles(testFile),
            test
        )
    }

    private fun runTestIfEnabledByDirective(
        testFile: Path,
        directive: EnableOrDisableTestDirective,
        directivePosition: DirectivePosition,
        additionalFiles: List<Path>,
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
                    handleTestWithWrongDirective(testPasses = false, testFile, directive, directivePosition, additionalFiles)
                }
                throw e
            }
            return
        }

        if (!testIsEnabled) {
            handleTestWithWrongDirective(testPasses = true, testFile, directive, directivePosition, additionalFiles)
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    private fun handleTestWithWrongDirective(
        testPasses: Boolean,
        testFile: Path,
        directive: EnableOrDisableTestDirective,
        directivePosition: DirectivePosition,
        additionalFiles: List<Path>,
    ) {
        val verb = when (testPasses) {
            false -> "do not pass"
            true -> "passes"
        }

        val directiveIsOutdated = when {
            !testPasses && directive is EnableOrDisableTestDirective.Enable -> true
            testPasses && directive is EnableOrDisableTestDirective.Disable -> true
            else -> false
        }

        val directiveIsMissing = !directiveIsOutdated

        if (directiveIsMissing && INSERT_DIRECTIVE_AUTOMATICALLY) {
            testFile.insertDirectivesToFileAndAdditionalFile(directive, additionalFiles, directivePosition)
            val filesWithDirectiveAdded = buildList {
                add(testFile.fileName.toString())
                addAll(additionalFiles)
            }
            throw AssertionError(
                "Looks like the test $verb, ${directive.directiveText} was added to the ${filesWithDirectiveAdded.joinToString()}"
            )
        }

        if (directiveIsOutdated) {
            throw AssertionError(
                "Looks like the test $verb, please ${directive.fixDirectiveMessage} the ${testFile.fileName}"
            )
        }
    }

    private fun computeAdditionalFilesByExtensions(mainTestFile: Path, additionalFilesExtensions: List<String>): List<Path> {
        return additionalFilesExtensions.mapNotNull { mainTestFile.getSiblingFile(it) }
    }

    private fun Path.insertDirectivesToFileAndAdditionalFile(
        directive: EnableOrDisableTestDirective,
        additionalFiles: List<Path>,
        directivePosition: DirectivePosition,
    ) {
        insertDirective(directive, directivePosition)
        additionalFiles.forEach { it.insertDirective(directive, directivePosition) }
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
        const val FIR_COMPARISON_MULTILINE_COMMENT = "/* FIR_COMPARISON */"

        const val IGNORE_FIR = "// IGNORE_FIR"
        const val IGNORE_FIR_MULTILINE_COMMENT = "/* IGNORE_FIR */"

        const val FIX_ME = "// FIX_ME: "
        const val FIR_IDENTICAL = "// FIR_IDENTICAL"

        const val IGNORE_FE10_BINDING_BY_FIR = "// IGNORE_FE10_BINDING_BY_FIR"
    }

    enum class DirectivePosition {
        FIRST_LINE_IN_FILE, LAST_LINE_IN_FILE
    }

    private val isTeamCityBuild: Boolean
        get() = System.getenv("TEAMCITY_VERSION") != null
                || KtUsefulTestCase.IS_UNDER_TEAMCITY


    fun getFirTestFile(originalTestFile: File): File {
        if (originalTestFile.readLines().any { it.startsWith(DIRECTIVES.FIR_IDENTICAL) }) {
            return originalTestFile
        }
        val firTestFile = deriveFirTestFile(originalTestFile)
        if (!firTestFile.exists()) {
            FileUtil.copy(originalTestFile, firTestFile)
        }
        return firTestFile
    }

    fun getFirTestFileIfFirPassing(originalTestFile: File, passingDirective: String): File {
        if (!InTextDirectivesUtils.isDirectiveDefined(originalTestFile.readText(), passingDirective)) {
            return originalTestFile
        }
        return getFirTestFile(originalTestFile)
    }


    fun cleanUpIdenticalFirTestFile(
        originalTestFile: File,
        firTestFile: File = deriveFirTestFile(originalTestFile),
        additionalFileToMarkFirIdentical: File? = null
    ) {
        if (firTestFile.exists() && firTestFile.readText().trim() == originalTestFile.readText().trim()) {
            val message = if (isTeamCityBuild) {
                "Please remove $firTestFile and add // FIR_IDENTICAL to test source file $originalTestFile"
            } else {
                // The FIR test file is identical with the original file. We should remove the FIR file and mark "FIR_IDENTICAL" in the
                // original file
                firTestFile.delete()
                originalTestFile.prependFirIdentical()
                additionalFileToMarkFirIdentical?.prependFirIdentical()

                "Deleted $firTestFile, added // FIR_IDENTICAL to test source file $originalTestFile"
            }
            KtAssert.fail(
                """
                        Dumps via FIR & via old FE are the same. 
                        $message
                        Please re-run the test now
                    """.trimIndent()
            )
        }
    }

    private fun File.prependFirIdentical() {
        val content = readText()
        writer().use {
            it.appendLine(DIRECTIVES.FIR_IDENTICAL)
            it.append(content)
        }
    }

    private fun deriveFirTestFile(originalTestFile: File) =
        originalTestFile.parentFile.resolve(originalTestFile.name.removeSuffix(".kt") + ".fir.kt")
}
