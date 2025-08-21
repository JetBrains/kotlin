/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.checkers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.TestExceptionsComparator
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.SpecTestLinkedType
import org.jetbrains.kotlin.spec.utils.models.AbstractSpecTest
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser
import org.jetbrains.kotlin.spec.utils.validators.DiagnosticTestTypeValidator
import org.jetbrains.kotlin.spec.utils.validators.SpecTestValidationException
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assert
import java.io.File
import java.util.regex.Matcher

@OptIn(ObsoleteTestInfrastructure::class)
abstract class AbstractDiagnosticsTestSpec : org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest() {
    companion object {
        private val withoutDescriptorsTestGroups = listOf(
            "linked/when-expression"
        )

        private const val DIAGNOSTICS_TESTDATA_PATH = "testData/diagnostics"
        private const val HELPERS_PATH = "$DIAGNOSTICS_TESTDATA_PATH/helpers"

        fun additionalKtFiles(specTest: AbstractSpecTest, project: Project): List<KtFile> {
            if (specTest.helpers == null) return emptyList()

            return specTest.helpers.map {
                val filename = "$it.kt"
                val helperContent = FileUtil.loadFile(File("$HELPERS_PATH/$filename"), true)


                KtTestUtil.createFile(filename, helperContent, project)
            }
        }
    }

    lateinit var specTest: AbstractSpecTest
    lateinit var testLinkedType: SpecTestLinkedType

    private var skipDescriptors = true

    private fun checkDirective(directive: String, testFiles: List<TestFile>) =
        testFiles.any { it.directives.contains(directive) }

    private fun enableDescriptorsGenerationIfNeeded(testFilePath: String) {
        skipDescriptors = withoutDescriptorsTestGroups.any {
            val testGroupAbsolutePath = File("$DIAGNOSTICS_TESTDATA_PATH/$it").absolutePath
            testFilePath.startsWith(testGroupAbsolutePath)
        }
    }

    override fun extractConfigurationKind(files: List<TestFile>): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    override fun skipDescriptorsValidation() = skipDescriptors

    override fun getKtFiles(testFiles: List<TestFile>, includeExtras: Boolean): List<KtFile> {
        val ktFiles = super.getKtFiles(testFiles, includeExtras) as ArrayList

        ktFiles.addAll(additionalKtFiles(specTest, project))

        return ktFiles
    }

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        val testFilePath = testDataFile.canonicalPath

        enableDescriptorsGenerationIfNeeded(testFilePath)

        CommonParser.parseSpecTest(testFilePath, files.associate { Pair(it.fileName, it.clearText) }).apply {
            specTest = first
            testLinkedType = second
        }

        if (GeneralConfiguration.PRINT_TEST_OUTPUTS_TO_STDOUT) {
            println(specTest)
        }

        val computeExceptionPoint: (Matcher?) -> Set<Int>? = l@{ matches ->
            if (matches == null) return@l null

            val lineNumber = matches.group("lineNumber").toInt()
            val symbolNumber = matches.group("symbolNumber").toInt()
            val filename = matches.group("filename")
            val fileContent = files.find { it.ktFile?.name == filename }!!.clearText
            val exceptionPosition = fileContent.lines().subList(0, lineNumber).joinToString("\n").length + symbolNumber
            val testCases = specTest.cases.byRanges[filename]
            val testCasesWithSamePosition = testCases!!.floorEntry(exceptionPosition).value

            return@l testCasesWithSamePosition.keys.toSet()
        }

        val exceptionsInCases = specTest.cases.byNumbers.entries.associate { it.key to it.value.exception }
        TestExceptionsComparator(testDataFile).run(
            specTest.exception,
            exceptionsInCases,
            computeExceptionPoint,
            printExceptionsToConsole = GeneralConfiguration.PRINT_TEST_OUTPUTS_TO_STDOUT
        ) {
            super.analyzeAndCheck(testDataFile, files)
        }
    }

    override fun performAdditionalChecksAfterDiagnostics(
        testDataFile: File,
        testFiles: List<TestFile>,
        moduleFiles: Map<TestModule?, List<TestFile>>,
        moduleDescriptors: Map<TestModule?, ModuleDescriptorImpl>,
        moduleBindings: Map<TestModule?, BindingContext>,
        languageVersionSettingsByModule: Map<TestModule?, LanguageVersionSettings>
    ) {
        val diagnosticValidator = try {
            DiagnosticTestTypeValidator(testFiles, testDataFile, specTest)
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.description)
            return
        }

        try {
            diagnosticValidator.validatePathConsistency(testLinkedType)
            diagnosticValidator.validateTestType()
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.description)
        } finally {
            if (GeneralConfiguration.PRINT_TEST_OUTPUTS_TO_STDOUT) {
                diagnosticValidator.printDiagnosticStatistic()
            }
        }
    }

    // Force language version 1.9 if K1 is used, otherwise the K1 compiler will pretend that it has all new language features
    // enabled, in particular JvmDefaultEnableByDefault, which makes it report an error EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE
    // in `compiler/tests-spec/testData/diagnostics/notLinked/dfa/pos/15.kt`.
    override fun defaultLanguageVersionSettings(): LanguageVersionSettings =
        CompilerTestLanguageVersionSettings(DEFAULT_DIAGNOSTIC_TESTS_FEATURES, ApiVersion.KOTLIN_1_9, LanguageVersion.KOTLIN_1_9)
}
