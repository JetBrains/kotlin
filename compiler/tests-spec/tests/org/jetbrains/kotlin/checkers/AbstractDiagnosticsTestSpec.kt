/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.*
import org.junit.Assert
import java.io.File

abstract class AbstractDiagnosticsTestSpec : AbstractDiagnosticsTest() {
    private lateinit var testValidator: DiagnosticSpecTestValidator

    override fun getConfigurationKind(): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        testValidator = DiagnosticSpecTestValidator(testDataFile)

        try {
            testValidator.validateByTestInfo()
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.reason.description)
        }

        testValidator.printTestInfo()

        this.setSkipTxtDirective(files)
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
