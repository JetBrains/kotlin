/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.TestExceptionsComparator
import org.jetbrains.kotlin.spec.models.AbstractSpecTest
import org.jetbrains.kotlin.spec.parsers.CommonParser
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.packagePattern
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTDATA_PATH
import org.jetbrains.kotlin.spec.validators.BlackBoxTestTypeValidator
import org.jetbrains.kotlin.spec.validators.SpecTestValidationException
import org.junit.Assert
import java.io.*

abstract class AbstractBlackBoxCodegenTestSpec : AbstractBlackBoxCodegenTest() {
    companion object {
        private const val CODEGEN_BOX_TESTDATA_PATH = "$TESTDATA_PATH/codegen/box"
        private const val HELPERS_PATH = "$CODEGEN_BOX_TESTDATA_PATH/helpers"
        private const val HELPERS_PACKAGE_VARIABLE = "<!PACKAGE!>"
    }

    private fun addPackageDirectiveToHelperFile(helperContent: String, packageName: String?) =
        helperContent.replace(HELPERS_PACKAGE_VARIABLE, if (packageName == null) "" else "package $packageName")

    private fun includeHelpers(wholeFile: File, files: MutableList<TestFile>, specTest: AbstractSpecTest) {
        if (specTest.helpers == null) return

        val fileContent = FileUtil.loadFile(wholeFile, true)
        val packageName = packagePattern.matcher(fileContent).let {
            if (it.find()) it.group("packageName") else null
        }

        specTest.helpers.forEach {
            val filename = "$it.kt"
            val helperContent = FileUtil.loadFile(File("$HELPERS_PATH/$filename"), true)
            files.add(
                TestFile(filename, addPackageDirectiveToHelperFile(helperContent, packageName))
            )
        }
    }

    override fun doMultiFileTest(wholeFile: File, files: MutableList<TestFile>, javaFilesDir: File?) {
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

        println(specTest)

        includeHelpers(wholeFile, files, specTest)

        if (specTest.exception == null) {
            super.doMultiFileTest(wholeFile, files, javaFilesDir)
        } else {
            TestExceptionsComparator(wholeFile).run(specTest.exception) {
                super.doMultiFileTest(wholeFile, files, javaFilesDir)
            }
        }
    }
}
