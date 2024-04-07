/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.CHECK_BYTECODE_TEXT
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.TREAT_AS_ONE_FILE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultDirectives
import org.jetbrains.kotlin.test.services.isKtFile

class BytecodeTextHandler(testServices: TestServices, private val shouldEnableExplicitly: Boolean = false) :
    JvmBinaryArtifactHandler(testServices) {

    companion object {
        private const val IGNORED_PREFIX = "helpers/"
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (shouldEnableExplicitly && CHECK_BYTECODE_TEXT !in module.directives) return

        val targetBackend = module.targetBackend!!
        val isIgnored = testServices.codegenSuppressionChecker.failuresInModuleAreIgnored(module)
        val files = module.files.filter { it.isKtFile }
        if (files.size > 1 && TREAT_AS_ONE_FILE !in module.directives) {
            processMultiFileTest(files, info, targetBackend, !isIgnored)
        } else {
            val file = files.first { !it.isAdditional }
            val expected = readExpectedOccurrences(file.originalContent.split("\n"))
            val actual = info.classFileFactory.createText(IGNORED_PREFIX)
            checkGeneratedTextAgainstExpectedOccurrences(
                actual,
                expected,
                targetBackend,
                !isIgnored,
                assertions,
                inlineScopesNumbersEnabled()
            )
        }
    }

    private fun processMultiFileTest(
        files: List<TestFile>,
        info: BinaryArtifacts.Jvm,
        targetBackend: TargetBackend,
        reportProblems: Boolean
    ) {
        val expectedOccurrencesByOutputFile = LinkedHashMap<String, List<OccurrenceInfo>>()
        val globalOccurrences = ArrayList<OccurrenceInfo>()
        for (file in files) {
            readExpectedOccurrencesForMultiFileTest(file.name, file.originalContent, expectedOccurrencesByOutputFile, globalOccurrences)
        }

        if (globalOccurrences.isNotEmpty()) {
            val generatedText = info.classFileFactory.createText()
            checkGeneratedTextAgainstExpectedOccurrences(
                generatedText,
                globalOccurrences,
                targetBackend,
                reportProblems,
                assertions,
                inlineScopesNumbersEnabled()
            )
        }

        val generatedByFile = info.classFileFactory.createTextForEachFile()
        for (expectedOutputFile in expectedOccurrencesByOutputFile.keys) {
            assertTextWasGenerated(expectedOutputFile, generatedByFile, assertions)
            val generatedText = generatedByFile[expectedOutputFile]!!
            val expectedOccurrences = expectedOccurrencesByOutputFile[expectedOutputFile]!!
            checkGeneratedTextAgainstExpectedOccurrences(
                generatedText,
                expectedOccurrences,
                targetBackend,
                reportProblems,
                assertions,
                inlineScopesNumbersEnabled()
            )
        }
    }

    private fun inlineScopesNumbersEnabled(): Boolean {
        return LanguageSettingsDirectives.USE_INLINE_SCOPES_NUMBERS in testServices.defaultDirectives
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
