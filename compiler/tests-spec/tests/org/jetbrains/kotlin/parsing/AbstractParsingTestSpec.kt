/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.spec.parsers.CommonParser
import org.jetbrains.kotlin.spec.validators.*
import org.junit.Assert
import java.io.File

abstract class AbstractParsingTestSpec : AbstractParsingTest() {
    override fun doParsingTest(filePath: String) {
        val (specTest, testLinkedType) = CommonParser.parseSpecTest(
            filePath,
            mapOf("main.kt" to FileUtil.loadFile(File(filePath), true))
        )

        println(specTest)

        super.doParsingTest(filePath, CommonParser::testInfoFilter)

        val psiTestValidator = ParsingTestTypeValidator(myFile, File(filePath), specTest)

        try {
            psiTestValidator.validatePathConsistency(testLinkedType)
            psiTestValidator.validateTestType()
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.description)
        }
    }
}
