/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.tasks

import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.spec.parsing.AbstractParsingTestSpec
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TEST_PATH
import org.jetbrains.kotlin.spec.utils.SectionsJsonMapGenerator
import org.jetbrains.kotlin.spec.utils.TestsJsonMapGenerator

fun generateTests() {
    generateTestGroupSuiteWithJUnit4 {
        testGroup(SPEC_TEST_PATH, SPEC_TESTDATA_PATH) {
            testClass<AbstractParsingTestSpec> {
                model(
                    relativeRootPath = "psi",
                    testMethod = "doParsingTest",
                    excludeDirs = listOf("helpers", "templates") + detectDirsWithTestsMapFileOnly("psi")
                )
            }
        }
    }
}

fun main() {
    TestsJsonMapGenerator.buildTestsMapPerSection()
    SectionsJsonMapGenerator.writeSectionsMapJsons()
    generateTests()
}
