/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.codegen

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.TestExceptionsComparator
import org.jetbrains.kotlin.codegen.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.models.AbstractSpecTest
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser
import org.jetbrains.kotlin.spec.utils.parsers.CommonPatterns.packagePattern
import org.jetbrains.kotlin.spec.utils.validators.BlackBoxTestTypeValidator
import org.jetbrains.kotlin.spec.utils.validators.SpecTestValidationException
import org.junit.Assert
import java.io.File

@OptIn(ObsoleteTestInfrastructure::class)
abstract class AbstractBlackBoxCodegenTestSpec : AbstractBlackBoxCodegenTest() {
    companion object {
        private const val CODEGEN_BOX_TESTDATA_PATH = "$SPEC_TESTDATA_PATH/codegen/box"
        private const val HELPERS_PATH = "$CODEGEN_BOX_TESTDATA_PATH/helpers"
        private const val HELPERS_PACKAGE_VARIABLE = "<!PACKAGE!>"
    }

    private fun addPackageDirectiveToHelperFile(helperContent: String, packageName: String?) =
        helperContent.replace(HELPERS_PACKAGE_VARIABLE, if (packageName == null) "" else "package $packageName")

    private fun includeHelpers(wholeFile: File, files: List<TestFile>, specTest: AbstractSpecTest): List<TestFile> {
        if (specTest.helpers == null) return files

        val fileContent = FileUtil.loadFile(wholeFile, true)
        val packageName = packagePattern.matcher(fileContent).let {
            if (it.find()) it.group("packageName") else null
        }

        return files + specTest.helpers.map {
            val filename = "$it.kt"
            val helperContent = FileUtil.loadFile(File("$HELPERS_PATH/$filename"), true)
            TestFile(filename, addPackageDirectiveToHelperFile(helperContent, packageName))
        }
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        val (specTest, testLinkedType) = CommonParser.parseSpecTest(
            wholeFile.canonicalPath,
            mapOf("main.kt" to FileUtil.loadFile(wholeFile, true))
        )

        val validator = BlackBoxTestTypeValidator(wholeFile, specTest)

        try {
            validator.validatePathConsistency(testLinkedType)
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.description)
        }

        if (GeneralConfiguration.PRINT_TEST_OUTPUTS_TO_STDOUT) {
            println(specTest)
        }

        val filesWithHelpers = includeHelpers(wholeFile, files, specTest)

        val runTest = { super.doMultiFileTest(wholeFile, filesWithHelpers, specTest.unexpectedBehavior) }

        if (specTest.exception == null) {
            runTest()
        } else {
            TestExceptionsComparator(wholeFile).run(
                specTest.exception,
                printExceptionsToConsole = GeneralConfiguration.PRINT_TEST_OUTPUTS_TO_STDOUT,
                runTest
            )
        }
    }
}
