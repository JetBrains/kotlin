/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    generateTestGroupSuiteWithJUnit4(args, mainClassName) {
        testGroup("compiler/fir/raw-fir/light-tree2fir/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractLightTree2FirConverterTestCase> {
                // TODO(KT-77583): support REPL snippets in light tree parser.
                model("rawBuilder", pattern = TestGeneratorUtil.KT_OR_KTS, excludedPattern = TestGeneratorUtil.REPL_KTS)
            }
        }
    }
}
