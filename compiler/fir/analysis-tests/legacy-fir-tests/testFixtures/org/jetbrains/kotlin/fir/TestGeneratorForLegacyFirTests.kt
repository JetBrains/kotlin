/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.java.AbstractFirOldFrontendLightClassesTest
import org.jetbrains.kotlin.fir.java.AbstractFirTypeEnhancementTest
import org.jetbrains.kotlin.fir.java.AbstractOwnFirTypeEnhancementTest
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    generateTestGroupSuite(args, mainClassName) {
        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/testData") {
            testClass<AbstractFirTypeEnhancementTest> {
                model("loadJava/compiledJava", extension = "java")
            }
        }

        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractOwnFirTypeEnhancementTest> {
                model("enhancement", extension = "java")
            }

            testClass<AbstractFirOldFrontendLightClassesTest> {
                model("lightClasses")
            }
        }
    }
}
