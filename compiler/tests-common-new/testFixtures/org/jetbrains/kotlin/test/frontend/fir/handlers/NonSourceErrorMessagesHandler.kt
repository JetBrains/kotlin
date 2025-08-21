/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.handlers.assertFileDoesntExist
import org.jetbrains.kotlin.test.cli.CliDirectives.CHECK_COMPILER_OUTPUT
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.utils.MessageCollectorForCompilerTests
import org.jetbrains.kotlin.test.utils.withExtension

/**
 * This handler checks all message collectors from all modules for diagnostics not attached to specific source location
 * (e.g. for diagnostics about some unresolved dependencies) and dumps them to `.cli.out`/`.cli.fir.out` file
 */
class NonSourceErrorMessagesHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun check(failedAssertions: List<WrappedException>) {
        if (CHECK_COMPILER_OUTPUT !in testServices.moduleStructure.allDirectives) return

        val dump = testServices.moduleStructure.modules.map { module ->
            val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
            val messageCollector = configuration.messageCollector as MessageCollectorForCompilerTests
            messageCollector.nonSourceMessages.joinToString("\n")
        }.filter { it.isNotEmpty() }.joinToString("\n")
        check(dump)
    }

    fun check(resultingDump: String) {
        val sourceFile = testServices.moduleStructure.originalTestDataFiles.first()
        val defaultOutFile = sourceFile.withExtension(".cli.out")
        val firOutFile = sourceFile.withExtension(".cli.fir.out")

        val isFir = testServices.defaultsProvider.frontendKind == FrontendKinds.FIR

        val outFile = if (isFir && firOutFile.exists()) firOutFile else defaultOutFile

        val assertions = testServices.assertions
        if (resultingDump.isEmpty()) {
            if (outFile == firOutFile || defaultOutFile.exists()) {
                assertions.assertEqualsToFile(firOutFile, "")
            } else {
                assertions.assertFileDoesntExist(outFile, CHECK_COMPILER_OUTPUT)
            }
            return
        }

        val actualOutput = CompilerTestUtil.normalizeCompilerOutput(
            resultingDump,
            testServices.temporaryDirectoryManager.rootDir.path,
        )
        assertions.assertEqualsToFile(outFile, actualOutput)

        if (outFile != defaultOutFile) {
            if (outFile.readText().trim() == defaultOutFile.readText().trim()) assertions.fail {
                "Classic and FIR golden files are identical. Remove $outFile."
            }
        }
    }
}

