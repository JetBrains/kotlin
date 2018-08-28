/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing

import org.jetbrains.kotlin.spec.validators.*
import org.junit.Assert

abstract class AbstractParsingTestSpec : AbstractParsingTest() {
    private lateinit var testValidator: AbstractSpecTestValidator<out AbstractSpecTest>

    override fun doParsingTest(filePath: String) {
        testValidator = AbstractSpecTestValidator.getInstanceByType(filePath)

        try {
            testValidator.parseTestInfo()
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.description)
        }

        testValidator.printTestInfo()

        super.doParsingTest(filePath, testValidator::testInfoFilter)

        try {
            testValidator.validateTestType(computedTestType = ParsingTestTypeValidator.computeTestType(myFile))
        } catch (e: SpecTestValidationException) {
            Assert.fail(e.description)
        }
    }
}
