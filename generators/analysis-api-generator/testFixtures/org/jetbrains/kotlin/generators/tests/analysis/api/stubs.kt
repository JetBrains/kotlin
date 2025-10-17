/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.decompiler.psi.common.AbstractDecompiledCommonTextTest
import org.jetbrains.kotlin.analysis.decompiler.psi.js.AbstractDecompiledJsTextTest
import org.jetbrains.kotlin.analysis.decompiler.psi.jvm.AbstractDecompiledJvmTextTest
import org.jetbrains.kotlin.analysis.stubs.AbstractSourceStubsTest
import org.jetbrains.kotlin.analysis.stubs.common.AbstractCompiledCommonStubsTest
import org.jetbrains.kotlin.analysis.stubs.js.AbstractCompiledJsStubsTest
import org.jetbrains.kotlin.analysis.stubs.jvm.AbstractCompiledJvmStubsTest
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

internal fun TestGroupSuite.generateStubsTests() {
    testGroup("analysis/stubs/tests-gen", "compiler/testData") {
        testClass<AbstractSourceStubsTest> {
            model("psi", pattern = TestGeneratorUtil.KT_OR_KTS_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractCompiledJvmStubsTest> {
            model("psi", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractCompiledJsStubsTest> {
            // 1.9 is not supported for non-JVM platforms, so k1 is excluded
            model("psi", excludeDirs = listOf("k1"), pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractCompiledCommonStubsTest> {
            // 1.9 is not supported for non-JVM platforms, so k1 is excluded
            model("psi", excludeDirs = listOf("k1"), pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractDecompiledJvmTextTest> {
            model("psi", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractDecompiledJsTextTest> {
            // 1.9 is not supported for non-JVM platforms, so k1 is excluded
            model("psi", excludeDirs = listOf("k1"), pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractDecompiledCommonTextTest> {
            // 1.9 is not supported for non-JVM platforms, so k1 is excluded
            model("psi", excludeDirs = listOf("k1"), pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }
}
