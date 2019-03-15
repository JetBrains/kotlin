/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.TestExceptionsComparator
import org.jetbrains.kotlin.spec.parsers.CommonParser
import org.jetbrains.kotlin.spec.validators.*
import org.junit.Assert
import java.io.File

abstract class AbstractParsingTestSpec : AbstractParsingTest() {
    override fun doParsingTest(filePath: String) {
        val file = File(filePath)
        val (specTest, testLinkedType) = CommonParser.parseSpecTest(
            filePath,
            mapOf("main.kt" to FileUtil.loadFile(file, true))
        )

        println(specTest)

        if (specTest.exception == null) {
            super.doParsingTest(filePath, CommonParser::testInfoFilter)
        } else {
            TestExceptionsComparator(file).run(specTest.exception) {
                super.doParsingTest(filePath, CommonParser::testInfoFilter)
            }
        }

        try {
            val psiTestValidator = ParsingTestTypeValidator(myFile, File(filePath), specTest)
            psiTestValidator.validatePathConsistency(testLinkedType)
            psiTestValidator.validateTestType()
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.description)
        }
    }
}
