/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.codegen.MODULE_INFO_FILE
import org.jetbrains.kotlin.codegen.ModuleInfoParser
import org.jetbrains.kotlin.codegen.PROJECT_INFO_FILE
import org.jetbrains.kotlin.codegen.ProjectInfoParser
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.TestStructure.ModuleUnderTest
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.assertExists
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

abstract class PartialLinkageTestStructureExtractor : KlibCompilerInvocationTestUtils.TestStructureExtractor {
    protected abstract val buildDir: File

    // Platform-specific `TestMode()` constructor parameters.
    protected abstract val testModeConstructorParameters: Map<String, String>

    // Customize the source code of a module before compiling it to a KLIB.
    protected abstract fun customizeModuleSources(moduleName: String, moduleSourceDir: File)

    override fun extractTestStructure(testDataPath: File): KlibCompilerInvocationTestUtils.TestStructure {
        assertTrue(testDataPath.isDirectory) { "Not a directory: $testDataPath" }

        val projectInfoFile = File(testDataPath, PROJECT_INFO_FILE)
        val projectInfo = ProjectInfoParser(projectInfoFile).parse(testDataPath.name)

        val modules = mutableMapOf<String, ModuleUnderTest>()

        projectInfo.modules.forEach { moduleName ->
            val moduleTestDataDir = File(testDataPath, moduleName)
            assertExists(moduleTestDataDir)

            val moduleInfoFile = File(moduleTestDataDir, MODULE_INFO_FILE)
            val moduleInfo = ModuleInfoParser(moduleInfoFile).parse(moduleName)

            val moduleBuildDir = buildDir.resolve(moduleName)
            if (moduleBuildDir.exists()) moduleBuildDir.deleteRecursively()

            val moduleSourceDir = moduleBuildDir.resolve(SOURCE_DIR_NAME).apply { mkdirs() }
            val moduleOutputDir = moduleBuildDir.resolve(OUTPUT_DIR_NAME).apply { mkdirs() }

            // Populate the source dir with *.kt files.
            copySources(from = moduleTestDataDir, to = moduleSourceDir)

            // Customize the source dir if necessary.
            customizeModuleSources(moduleName, moduleSourceDir)

            // Include PL utils into the main module.
            if (moduleName == MAIN_MODULE_NAME) {
                val utilsDir = testDataPath.parentFile.parentFile.resolve(PL_UTILS_DIR)
                assertExists(utilsDir)

                copySources(from = utilsDir, to = moduleSourceDir) { contents ->
                    contents.replace(
                        TEST_MODE_PLACEHOLDER,
                        buildString {
                            append("TestMode(")
                            testModeConstructorParameters.entries.joinTo(this) { it.key + " = " + it.value }
                            append(")")
                        }
                    )
                }
            }

            modules[moduleName] = ModuleUnderTest(
                moduleInfo,
                testDataDir = moduleTestDataDir,
                sourceDir = moduleSourceDir,
                outputDir = moduleOutputDir,
            )
        }

        return KlibCompilerInvocationTestUtils.TestStructure(
            projectInfo = projectInfo,
            modules = modules,
        )
    }

    protected fun copySources(from: File, to: File, patchSourceFile: ((String) -> String)? = null) {
        var anyFilePatched = false

        from.walk().filter { it.isFile && it.extension in knownSourceExtensions }.forEach { sourceFile ->
            val destFile = to.resolve(sourceFile.relativeTo(from))
            destFile.parentFile.mkdirs()
            sourceFile.copyTo(destFile)

            if (patchSourceFile != null) {
                val originalContents = destFile.readText()
                val patchedContents = patchSourceFile(originalContents)
                if (originalContents != patchedContents) {
                    anyFilePatched = true
                    destFile.writeText(patchedContents)
                }
            }
        }

        check(patchSourceFile == null || anyFilePatched) { "No source files have been patched" }
    }

    companion object {
        const val MAIN_MODULE_NAME = "main"

        private const val SOURCE_DIR_NAME = "sources"
        private const val OUTPUT_DIR_NAME = "outputs"

        private const val PL_UTILS_DIR = "__utils__"
        private const val TEST_MODE_PLACEHOLDER = "__UNKNOWN_TEST_MODE__"
        private val knownSourceExtensions = setOf("kt", "js", "def", "h", "c", "cpp")
    }
}
