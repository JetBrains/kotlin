/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.TestExceptionsComparator
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.spec.models.AbstractSpecTest
import org.jetbrains.kotlin.spec.SpecTestLinkedType
import org.jetbrains.kotlin.spec.parsers.CommonParser
import org.jetbrains.kotlin.spec.validators.*
import org.jetbrains.kotlin.test.*
import org.junit.Assert
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class AbstractDiagnosticsTestSpec : AbstractDiagnosticsTest() {
    companion object {
        // map of pairs: source helper filename - target helper filename
        private val directives = mapOf(
            "WITH_BASIC_TYPES" to "basicTypes.kt",
            "WITH_CLASSES" to "classes.kt",
            "WITH_ENUM_CLASSES" to "enumClasses.kt",
            "WITH_SEALED_CLASSES" to "sealedClasses.kt",
            "WITH_FUNCTIONS" to "functions.kt",
            "WITH_OBJECTS" to "objects.kt",
            "WITH_TYPEALIASES" to "typeAliases.kt",
            "WITH_CONTRACT_FUNCTIONS" to "contractFunctions.kt"
        )

        private val withoutDescriptorsTestGroups = listOf(
            "linked/when-expression"
        )

        private const val MODULE_PATH = "compiler/tests-spec"
        private const val DIAGNOSTICS_TESTDATA_PATH = "$MODULE_PATH/testData/diagnostics"
        private const val HELPERS_PATH = "$DIAGNOSTICS_TESTDATA_PATH/helpers"
        private val exceptionPattern =
            Pattern.compile("""Exception while analyzing expression at \((?<lineNumber>\d+),(?<symbolNumber>\d+)\) in /(?<filename>.*?)$""")
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

    override fun getConfigurationKind() = ConfigurationKind.ALL

    override fun skipDescriptorsValidation() = skipDescriptors

    override fun getKtFiles(testFiles: List<TestFile>, includeExtras: Boolean): List<KtFile> {
        val ktFiles = super.getKtFiles(testFiles, includeExtras) as ArrayList

        if (includeExtras) {
            for ((name, filename) in directives) {
                if (checkDirective(name, testFiles)) {
                    val declarations = File("$HELPERS_PATH/$filename").readText()
                    ktFiles.add(KotlinTestUtils.createFile(filename, declarations, project))
                }
            }
        }

        return ktFiles
    }

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        val testFilePath = testDataFile.canonicalPath

        enableDescriptorsGenerationIfNeeded(testFilePath)

        CommonParser.parseSpecTest(testFilePath, files.associate { Pair(it.fileName, it.clearText) }).apply {
            specTest = first
            testLinkedType = second
        }

        println(specTest)

        val checkUnexpectedBehaviour: (Matcher?) -> Pair<Boolean, Set<Int>?> = l@{ matches ->
            if (specTest.unexpectedBehavior) return@l Pair(true, null)
            if (matches == null) return@l Pair(false, null)

            val lineNumber = matches.group("lineNumber").toInt()
            val symbolNumber = matches.group("symbolNumber").toInt()
            val filename = matches.group("filename")
            val fileContent = files.find { it.ktFile?.name == filename }!!.clearText
            val exceptionPosition = fileContent.lines().subList(0, lineNumber).joinToString("\n").length + symbolNumber
            val testCases = specTest.cases.byRanges[filename]
            val testCasesWithSamePosition = testCases!!.floorEntry(exceptionPosition).value

            return@l Pair(testCasesWithSamePosition.all { it.value.unexpectedBehavior }, testCasesWithSamePosition.keys.toSet())
        }

        TestExceptionsComparator(testDataFile).runAndCompareWithExpected(checkUnexpectedBehaviour) {
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
            diagnosticValidator.printDiagnosticStatistic()
        }
    }
}
