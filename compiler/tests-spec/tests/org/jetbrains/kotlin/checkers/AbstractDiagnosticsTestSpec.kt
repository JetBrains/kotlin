/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.*
import org.junit.Assert
import java.io.File

abstract class AbstractDiagnosticsTestSpec : AbstractDiagnosticsTest() {
    companion object {
        // map of pairs: source helper filename - target helper filename
        private val directives = mapOf(
            "WITH_BASIC_TYPES_PROVIDER" to Pair("basicTypesProvider.kt", "BASIC_TYPES_PROVIDER.kt")
        )

        private const val HELPERS_PATH = "./compiler/tests-spec/testData/helpers"
    }

    private lateinit var testValidator: DiagnosticSpecTestValidator

    private fun checkDirective(directive: String, testFiles: List<TestFile>): Boolean {
        var declare = false

        testFiles.forEach {
            declare = declare or it.directives.contains(directive)
        }

        return declare
    }

    override fun getConfigurationKind(): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    override fun skipDescriptorsValidation(): Boolean = true

    override fun getKtFiles(testFiles: List<TestFile>, includeExtras: Boolean): List<KtFile> {
        val ktFiles = super.getKtFiles(testFiles, includeExtras) as ArrayList

        if (includeExtras) {
            directives.forEach {
                if (checkDirective(it.key, testFiles)) {
                    val declarations = File("$HELPERS_PATH/${it.value.first}").readText()

                    ktFiles.add(KotlinTestUtils.createFile(it.value.second, declarations, project))
                }
            }
        }

        return ktFiles
    }

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        testValidator = DiagnosticSpecTestValidator(testDataFile)

        try {
            testValidator.validateByTestInfo()
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.reason.description)
        }

        testValidator.printTestInfo()

        super.analyzeAndCheck(testDataFile, files)
    }

    override fun performAdditionalChecksAfterDiagnostics(
        testDataFile: File,
        testFiles: List<TestFile>,
        moduleFiles: Map<TestModule?, List<TestFile>>,
        moduleDescriptors: Map<TestModule?, ModuleDescriptorImpl>,
        moduleBindings: Map<TestModule?, BindingContext>
    ) {
        try {
            testValidator.validateByDiagnostics(testFiles)
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.reason.description)
        } finally {
            testValidator.printDiagnosticStatistic()
        }
    }
}
