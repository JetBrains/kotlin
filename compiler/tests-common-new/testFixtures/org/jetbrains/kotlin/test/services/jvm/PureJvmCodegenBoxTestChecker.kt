/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.jvm

import com.google.common.io.Files
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.TARGET_BACKEND
import org.jetbrains.kotlin.test.impl.NonGroupingPhaseTestConfigurationImpl
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

/**
 * This checker ensures that test with `TARGET_BACKEND: JVM` is not located in the `codegen/box` directory.
 * Pure JVM tests should be located in the `codegen/boxJvm` instead.
 */
class PureJvmCodegenBoxTestChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    companion object {
        private const val BASE_CODEGEN_PREFIX = "compiler/testData/codegen"
        private const val CODEGEN_BOX = "$BASE_CODEGEN_PREFIX/box/"
        private const val CODEGEN_BOX_JVM = "$BASE_CODEGEN_PREFIX/boxJvm/"
    }

    override fun check(failedAssertions: List<WrappedException>) {
        val directives = testServices.moduleStructure.allDirectives
        val targetBackends = directives[TARGET_BACKEND].distinct()
        val hasSingleJvmTarget = targetBackends.singleOrNull().let { it == TargetBackend.JVM || it == TargetBackend.JVM_IR }

        @OptIn(TestInfrastructureInternals::class)
        val testDataPath = (testServices.testConfiguration as NonGroupingPhaseTestConfigurationImpl).originalBuilder.testDataPath

        if (testDataPath.startsWith(CODEGEN_BOX_JVM) && !hasSingleJvmTarget) {
//            moveToBoxJvm(from = CODEGEN_BOX_JVM, to = CODEGEN_BOX)
            testServices.assertions.fail { "Only pure JVM test should not be located in '$CODEGEN_BOX_JVM' directory.\nPlease move the test to '$CODEGEN_BOX' directory" }
        }

        if (testDataPath.startsWith(CODEGEN_BOX) && hasSingleJvmTarget) {
//            moveToBoxJvm(from = CODEGEN_BOX, to = CODEGEN_BOX_JVM)
            testServices.assertions.fail { "Pure JVM test should be located in '$CODEGEN_BOX_JVM' directory.\nPlease move the test there" }
        }
    }

    // Use this function for automatic test data move
    @Suppress("unused")
    private fun moveToBoxJvm(from: String, to: String) {
        val baseFile = testServices.moduleStructure.originalTestDataFiles.first()
        val baseName = baseFile.nameWithoutExtension + "." // to disambiguate common prefixes
        val oldDir = baseFile.parentFile
        val newDir = File(oldDir.absolutePath.replace(from, to))
        newDir.mkdirs()
        oldDir.list()?.filter { it.startsWith(baseName) }?.forEach {
            val file = oldDir.resolve(it)
            val newFile = newDir.resolve(it)
            Files.move(file, newFile)
        }
    }
}
