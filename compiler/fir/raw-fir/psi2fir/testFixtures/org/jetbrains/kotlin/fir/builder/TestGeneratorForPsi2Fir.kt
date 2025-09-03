/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    generateTestGroupSuiteWithJUnit4(args, mainClassName) {
        testGroup("compiler/fir/raw-fir/psi2fir/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractRawFirBuilderTestCase> {
                model("rawBuilder", testMethod = "doRawFirTest", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractRawFirBuilderLazyBodiesByAstTest> {
                model("rawBuilder", testMethod = "doRawFirTest", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractRawFirBuilderLazyBodiesByStubTest> {
                model("rawBuilder", testMethod = "doRawFirTest", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractRawFirBuilderSourceElementMappingTestCase> {
                model("sourceElementMapping", testMethod = "doRawFirTest")
            }
        }
    }
}
