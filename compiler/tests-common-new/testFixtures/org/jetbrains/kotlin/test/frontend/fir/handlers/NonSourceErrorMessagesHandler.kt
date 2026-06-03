/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.cli.CliDirectives.CHECK_COMPILER_OUTPUT
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.TestDumpDirectives
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.MessageCollectorForCompilerTests

/**
 * This handler checks all message collectors from all modules for diagnostics not attached to specific source location
 * (e.g. for diagnostics about some unresolved dependencies) and dumps them to `.cli.out` file
 */
class NonSourceErrorMessagesHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestDumpDirectives, CodegenTestDirectives)

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
        val actualDump =
            if (resultingDump.isEmpty()) null
            else CompilerTestUtil.normalizeCompilerOutput(resultingDump, testServices.temporaryDirectoryManager.rootDir.path)
        testServices.assertions.assertEqualsToDump(testServices.moduleStructure, extension = ".cli.out", actualDump)
    }
}

