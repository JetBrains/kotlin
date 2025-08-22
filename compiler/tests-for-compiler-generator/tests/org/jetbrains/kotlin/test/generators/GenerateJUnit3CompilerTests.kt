/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.compiler.plugins.AbstractPluginCliTests
import org.jetbrains.kotlin.fir.java.AbstractFirOldFrontendLightClassesTest
import org.jetbrains.kotlin.fir.java.AbstractFirTypeEnhancementTest
import org.jetbrains.kotlin.fir.java.AbstractOwnFirTypeEnhancementTest
import org.jetbrains.kotlin.fir.lightTree.AbstractLightTree2FirConverterTestCase
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun generateJUnit3CompilerTests(args: Array<String>, mainClassName: String?) {
    generateTestGroupSuite(args, mainClassName) {
        testGroup("compiler/fir/raw-fir/light-tree2fir/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractLightTree2FirConverterTestCase> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT_OR_KTS)
            }
        }

        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/testData") {
            testClass<AbstractFirTypeEnhancementTest> {
                model("loadJava/compiledJava", extension = "java")
            }
        }

        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractOwnFirTypeEnhancementTest> {
                model("enhancement", extension = "java")
            }
        }

        testGroup("compiler/fir/analysis-tests/legacy-fir-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractFirOldFrontendLightClassesTest> {
                model("lightClasses")
            }
        }

        testGroup("plugins/plugins-interactions-testing/tests-gen", "plugins/plugins-interactions-testing/testData") {
            testClass<AbstractPluginCliTests> {
                model("cli", extension = "args", testMethod = "doJvmTest", recursive = false)
            }
        }
    }
}
