/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_STATE_MACHINE
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_TAIL_CALL_OPTIMIZATION
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.WITH_COROUTINES
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class CoroutineHelpersSourceFilesProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    private val helpersPath = "diagnostics/helpers/coroutines"

    private val coroutineHelpersPath = "$helpersPath/CoroutineHelpers.kt"
    private val coroutineUtilPath = "$helpersPath/CoroutineUtil.kt"
    private val stateMachineCheckerPath = "$helpersPath/StateMachineChecker.kt"
    private val tailCallOptimizationCheckerPath = "$helpersPath/TailCallOptimizationChecker.kt"

    override val directiveContainers: List<DirectivesContainer> =
        listOf(AdditionalFilesDirectives)

    @OptIn(ExperimentalStdlibApi::class)
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        if (WITH_COROUTINES !in module.directives) return emptyList()
        return buildList {
            val classLoader = this::class.java.classLoader
            add(classLoader.getResource(coroutineHelpersPath)!!.toTestFile())
            if (CHECK_STATE_MACHINE in module.directives) {
                add(classLoader.getResource(coroutineUtilPath)!!.toTestFile())
                add(classLoader.getResource(stateMachineCheckerPath)!!.toTestFile())
            }
            if (CHECK_TAIL_CALL_OPTIMIZATION in module.directives) {
                add(classLoader.getResource(tailCallOptimizationCheckerPath)!!.toTestFile())
            }
        }
    }
}
