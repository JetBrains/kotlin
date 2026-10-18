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
        val filesWithBox = module.files.filter { containsBoxMethod(it, SourceContentView.ORIGINAL) }
        if (filesWithBox.isEmpty()) return emptyList()

        val allLauncherContents = mutableListOf<String>()
        val isGrouped = testServices.isGroupedNonIsolatedBatch(globalDirectives, testModuleStructure)

        for (fileWithBox in filesWithBox) {
            var boxFqName = detectPackage(fileWithBox, SourceContentView.ORIGINAL)?.let { "$it.$BOX_FUNCTION_NAME" } ?: BOX_FUNCTION_NAME
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
        val fileName = if (isGrouped) groupedLauncherFileName(module.name) else LAUNCHER_FILE_NAME
        val launcherFile = tempDir.resolve(fileName).also {
            it.writeText(launcherContent)
        }
        return listOf(launcherFile.toTestFile())
    }

    companion object {
        /**
         * Matches the name of the file facade class of a launcher: `__launcher__Kt` for an isolated test, and
         * `__launcher_<hash>__Kt` in a grouped batch (see [groupedLauncherFileName]).
         */
        const val LAUNCHER_FACADE_CLASS_NAME_PATTERN: String = "__launcher(?:_[0-9a-z]+)?__Kt"

        /**
         * The launchers of the tests of a grouped batch are compiled together, so each is named after the hash of its module name,
         * in base 36, as [LAUNCHER_FACADE_CLASS_NAME_PATTERN] expects.
         */
        fun groupedLauncherFileName(moduleName: String): String = "__launcher_${moduleName.hashCode().toUInt().toString(36)}__.kt"

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
