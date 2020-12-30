/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_STATE_MACHINE
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_TAIL_CALL_OPTIMIZATION
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.WITH_COROUTINES
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import java.io.File

class CoroutineHelpersSourceFilesProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        private const val HELPERS_PATH = "./compiler/testData/diagnostics/helpers/coroutines"
        private const val COROUTINE_HELPERS_PATH = "$HELPERS_PATH/CoroutineHelpers.kt"
        private const val STATE_MACHINE_CHECKER_PATH = "$HELPERS_PATH/StateMachineChecker.kt"
        private const val TAIL_CALL_OPTIMIZATION_CHECKER_PATH = "$HELPERS_PATH/TailCallOptimizationChecker.kt"
    }

    override val directives: List<DirectivesContainer> =
        listOf(AdditionalFilesDirectives)

    @OptIn(ExperimentalStdlibApi::class)
    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        if (WITH_COROUTINES !in module.directives) return emptyList()
        return buildList {
            add(File(COROUTINE_HELPERS_PATH).toTestFile())
            if (CHECK_STATE_MACHINE in module.directives) {
                add(File(STATE_MACHINE_CHECKER_PATH).toTestFile())
            }
            if (CHECK_TAIL_CALL_OPTIMIZATION in module.directives) {
                add(File(TAIL_CALL_OPTIMIZATION_CHECKER_PATH).toTestFile())
            }
        }
    }
}
