/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.spec.DiagnosticSpecTestValidator
import org.jetbrains.kotlin.spec.SpecTestValidationException
import org.jetbrains.kotlin.test.*
import org.junit.Assert
import java.io.File

abstract class AbstractDiagnosticsTestSpec : AbstractDiagnosticsTest() {
    companion object {
        // map of pairs: source helper filename - target helper filename
        private val directives = mapOf(
            "WITH_BASIC_TYPES" to Pair("basicTypes.kt", "BASIC_TYPES.kt"),
            "WITH_CLASSES" to Pair("classes.kt", "CLASSES.kt"),
            "WITH_ENUM_CLASSES" to Pair("enumClasses.kt", "ENUM_CLASSES.kt"),
            "WITH_SEALED_CLASSES" to Pair("sealedClasses.kt", "SEALED_CLASSES.kt"),
            "WITH_FUNS" to Pair("funs.kt", "FUNS.kt"),
            "WITH_OBJECTS" to Pair("objects.kt", "OBJECTS.kt"),
            "WITH_TYPEALIASES" to Pair("typeAliases.kt", "TYPE_ALIASES.kt")
        )

        private const val MODULE_PATH = "./compiler/tests-spec"
        private const val HELPERS_PATH = "$MODULE_PATH/testData/helpers/diagnostics"
    }

    private lateinit var testValidator: DiagnosticSpecTestValidator

    private fun checkDirective(directive: String, testFiles: List<TestFile>) =
        testFiles.any { it.directives.contains(directive) }

    override fun getConfigurationKind() = ConfigurationKind.ALL

    override fun skipDescriptorsValidation(): Boolean = true

    override fun getKtFiles(testFiles: List<TestFile>, includeExtras: Boolean): List<KtFile> {
        val ktFiles = super.getKtFiles(testFiles, includeExtras) as ArrayList

        if (includeExtras) {
            for ((name, filenames) in directives) {
                if (checkDirective(name, testFiles)) {
                    val (sourceFilename, targetFilename) = filenames
                    val declarations = File("$HELPERS_PATH/$sourceFilename").readText()

                    ktFiles.add(KotlinTestUtils.createFile(targetFilename, declarations, project))
                }
            }
        }

        return ktFiles
    }

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        testValidator = DiagnosticSpecTestValidator(testDataFile)

        try {
            testValidator.parseTestInfo()
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.description)
        }

        testValidator.printTestInfo()

        super.analyzeAndCheck(testDataFile, files)
    }

    override fun performAdditionalChecksAfterDiagnostics(
        testDataFile: File,
        testFiles: List<TestFile>,
        moduleFiles: Map<TestModule?, List<TestFile>>,
        moduleDescriptors: Map<TestModule?, ModuleDescriptorImpl>,
        moduleBindings: Map<TestModule?, BindingContext>,
        languageVersionSettingsByModule: Map<TestModule?, LanguageVersionSettings>
    ) {
        try {
            testValidator.validateTestType(testFiles)
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.description)
        } finally {
            testValidator.printDiagnosticStatistic()
        }
    }
}
