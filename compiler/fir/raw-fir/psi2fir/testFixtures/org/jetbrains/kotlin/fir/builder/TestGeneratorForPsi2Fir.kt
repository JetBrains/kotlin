/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    val testRoot = args[0]

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testRoot, "compiler/testData") {
            testClass<AbstractRawFirBuilderTestCase> {
                model("rawFir/rawBuilder", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractRawFirBuilderSourceElementMappingTestCase> {
                model("rawFir/sourceElementMapping")
            }
        }
    }
}
