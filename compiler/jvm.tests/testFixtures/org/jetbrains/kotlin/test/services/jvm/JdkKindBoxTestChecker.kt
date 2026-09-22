/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.jvm

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.JvmBoxRunner
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.impl.NonGroupingStageTestConfigurationImpl
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Checks that tests in `compiler/testData/codegen/box(Jvm)` do not use the [JvmEnvironmentConfigurationDirectives.JDK_KIND] directive.
 * Such tests should be declared in `compiler/testData/codegen/boxModernJdk` instead.
 *
 * The reason is that tests in `box`/`boxJvm` are also being run on different JDK versions (see `compiler/tests-different-jdk`), where it's
 * not immediately clear which JDK version to use to run the test. Another reason is that tests in `boxModernJdk` are split by JVM versions
 * and thus can one day be improved so that only one JVM is created for each JDK version (see [JvmBoxRunner.invokeBoxInSeparateProcess]),
 * which is harder to do with standard box tests.
 */
class JdkKindBoxTestChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    companion object {
        private const val BASE_CODEGEN_PREFIX = "compiler/testData/codegen"
        private const val CODEGEN_BOX = "$BASE_CODEGEN_PREFIX/box/"
        private const val CODEGEN_BOX_JVM = "$BASE_CODEGEN_PREFIX/boxJvm/"
        private const val CODEGEN_BOX_MODERN_JDK = "$BASE_CODEGEN_PREFIX/boxModernJdk/"
    }

    override fun check(thereWereFailures: Boolean) {
        val jdkKind = testServices.moduleStructure.allDirectives[JvmEnvironmentConfigurationDirectives.JDK_KIND]
        if (jdkKind.none { it.requiresSeparateProcess }) return

        @OptIn(TestInfrastructureInternals::class)
        val testDataPath = (testServices.testConfiguration as NonGroupingStageTestConfigurationImpl).originalBuilder.testDataPath
        if (testDataPath.startsWith(CODEGEN_BOX) || testDataPath.startsWith(CODEGEN_BOX_JVM)) {
            testServices.assertions.fail {
                "Using the JDK_KIND directive is not allowed in the '$CODEGEN_BOX' directory.\n" +
                        "Please move the test to '$CODEGEN_BOX_MODERN_JDK' directory or remove the JDK_KIND directive."
            }
        }
    }
}
