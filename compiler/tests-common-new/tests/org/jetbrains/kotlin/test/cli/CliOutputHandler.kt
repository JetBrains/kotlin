/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.cli

import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.backend.handlers.assertFileDoesntExist
import org.jetbrains.kotlin.test.cli.CliDirectives.CHECK_COMPILER_OUTPUT
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension

class CliOutputHandler(testServices: TestServices) : BinaryArtifactHandler<CliArtifact>(
    testServices,
    CliArtifact.Kind,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = true,
) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    private val multiModuleInfoDumper = MultiModuleInfoDumper()

    override fun processModule(module: TestModule, info: CliArtifact) {
        if (info.kotlinOutput.isEmpty()) return
        if (CHECK_COMPILER_OUTPUT !in module.directives) return
        multiModuleInfoDumper.builderForModule(module).append(info.kotlinOutput)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val sourceFile = testServices.moduleStructure.originalTestDataFiles.first()
        val defaultOutFile = sourceFile.withExtension(".out")
        val firOutFile = sourceFile.withExtension(".fir.out")

        val isFir = testServices.defaultsProvider.defaultFrontend == FrontendKinds.FIR

        val outFile = if (isFir && firOutFile.exists()) firOutFile else defaultOutFile

        if (multiModuleInfoDumper.isEmpty()) {
            if (outFile == firOutFile) {
                assertions.assertEqualsToFile(firOutFile, "")
            } else {
                assertions.assertFileDoesntExist(outFile, CHECK_COMPILER_OUTPUT)
            }
            return
        }

        val actualOutput = CompilerTestUtil.normalizeCompilerOutput(
            multiModuleInfoDumper.generateResultingDump(),
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
