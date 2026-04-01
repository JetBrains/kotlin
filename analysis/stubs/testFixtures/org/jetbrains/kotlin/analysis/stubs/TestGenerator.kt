/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import org.jetbrains.kotlin.analysis.decompiler.psi.common.AbstractDecompiledCommonTextTest
import org.jetbrains.kotlin.analysis.decompiler.psi.js.AbstractDecompiledJsTextTest
import org.jetbrains.kotlin.analysis.decompiler.psi.jvm.AbstractDecompiledJvmTextTest
import org.jetbrains.kotlin.analysis.stubs.common.AbstractCompiledCommonStubsTest
import org.jetbrains.kotlin.analysis.stubs.js.AbstractCompiledJsStubsTest
import org.jetbrains.kotlin.analysis.stubs.jvm.AbstractCompiledJvmStubsTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("analysis/stubs/tests-gen", "compiler/psi/psi-impl/testData") {
            testClass<AbstractSourceStubsTest> {
                model("psi", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractCompiledJvmStubsTest> {
                model("psi", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractCompiledJsStubsTest> {
                model("psi", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractCompiledCommonStubsTest> {
                model("psi", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractDecompiledJvmTextTest> {
                model("psi", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractDecompiledJsTextTest> {
                model("psi", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractDecompiledCommonTextTest> {
                model("psi", pattern = TestGeneratorUtil.KT)
            }
        }
    }
}