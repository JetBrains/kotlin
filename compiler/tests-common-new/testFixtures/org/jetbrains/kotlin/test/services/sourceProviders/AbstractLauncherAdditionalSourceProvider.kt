/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives.ESCAPE_MODULE_NAME
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.testInfo

private const val LAUNCHER_FILE_NAME = "__launcher__.kt"
private const val BOX_FUNCTION_NAME = "box"

abstract class AbstractLauncherAdditionalSourceProvider(testServices: TestServices) : MainFunctionForBlackBoxTestsSourceProvider(testServices) {

    protected abstract fun generateLauncherContent(boxFqName: String, expectedResult: String = "OK"): String

    protected open fun generateLauncherContent(boxFqName: String, testFile: TestFile, expectedResult: String = "OK"): String =
        generateLauncherContent(boxFqName, expectedResult)

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        val filesWithBox = module.files.filter { containsBoxMethod(it.originalContent) }
        if (filesWithBox.isEmpty()) return emptyList()

        val allLauncherContents = mutableListOf<String>()
        val isGrouped = testServices.isGroupedNonIsolatedBatch(globalDirectives, testModuleStructure)

        for (fileWithBox in filesWithBox) {
            var boxFqName = detectPackage(fileWithBox)?.let { "$it.$BOX_FUNCTION_NAME" } ?: BOX_FUNCTION_NAME
            if (isGrouped) {
                val additionalPackage = BatchingPackageInserter.computePackage(testServices.testInfo)
                if (!boxFqName.startsWith(additionalPackage)) {
                    boxFqName = "$additionalPackage.$boxFqName"
                }
            }
            allLauncherContents.add(generateLauncherContent(boxFqName, fileWithBox))
        }

        val launcherContent = allLauncherContents.joinToString("\n\n")

        val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("launcher")
        val fileName = if (isGrouped) {
            "__launcher_${module.name.hashCode().toUInt().toString(36)}__.kt"
        } else {
            LAUNCHER_FILE_NAME
        }
        val launcherFile = tempDir.resolve(fileName).also {
            it.writeText(launcherContent)
        }
        return listOf(launcherFile.toTestFile())
    }

    companion object {
        /**
         * Returns `true` when the test is being executed by the grouping engine (`ESCAPE_MODULE_NAME`
         * directive is set globally) and is *not* isolated from its batch by
         * [shouldIsolateTestInGroupingConfiguration]. This is the "groupedBatch" path of the WASM test second-stage
         * facade: tests in this group share a single `ProxyBatchLauncher.kt` synthesized by the
         * grouping facade, and any per-test launcher sources are unnecessary.
         */
        fun TestServices.isGroupedNonIsolatedBatch(
            globalDirectives: RegisteredDirectives,
            testModuleStructure: TestModuleStructure,
        ): Boolean =
            ESCAPE_MODULE_NAME in globalDirectives &&
                    !shouldIsolateTestInGroupingConfiguration(testModuleStructure, fileGenerationPhase = true)
    }
}
