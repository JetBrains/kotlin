/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.backend.handlers.assertFileDoesntExist
import org.jetbrains.kotlin.test.cli.CliDirectives.CHECK_COMPILER_OUTPUT
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.MessageCollectorForCompilerTests
import org.jetbrains.kotlin.test.utils.withExtension

/**
 * This handler checks all message collectors from all modules for diagnostics not attached to specific source location
 * (e.g. for diagnostics about some unresolved dependencies) and dumps them to `.cli.out` file
 */
class NonSourceErrorMessagesHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun check(thereWereFailures: Boolean) {
        if (CHECK_COMPILER_OUTPUT !in testServices.moduleStructure.allDirectives) return

        val dump = testServices.moduleStructure.modules.map { module ->
            val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)

            @OptIn(MessageCollectorAccess::class)
            val messageCollector = configuration.messageCollector as MessageCollectorForCompilerTests
            messageCollector.nonSourceMessages.joinToString("\n")
        }.filter { it.isNotEmpty() }.joinToString("\n")
        check(dump)
    }

    fun check(resultingDump: String) {
        val sourceFile = testServices.moduleStructure.originalTestDataFiles.first()
        val outFile = sourceFile.withExtension(".cli.out")

        val assertions = testServices.assertions
        if (resultingDump.isEmpty()) {
            assertions.assertFileDoesntExist(outFile, CHECK_COMPILER_OUTPUT)
            return
        }

        val actualOutput = CompilerTestUtil.normalizeCompilerOutput(
            resultingDump,
            testServices.temporaryDirectoryManager.rootDir.path,
        )
        assertions.assertEqualsToFile(outFile, actualOutput)
    }
}

