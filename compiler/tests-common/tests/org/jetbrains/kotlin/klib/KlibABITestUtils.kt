/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.klib.KlibABITestUtils.ModuleBuildDirs.Companion.OUTPUT_DIR_NAME
import org.jetbrains.kotlin.klib.KlibABITestUtils.ModuleBuildDirs.Companion.SOURCE_DIR_NAME
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.jupiter.api.fail
import java.io.File

object KlibABITestUtils {
    interface TestConfiguration {
        val testDir: File
        val buildDir: File
        val stdlibFile: File
        val testModeName: String

        fun buildKlib(moduleName: String, moduleSourceDir: File, dependencies: Dependencies, klibFile: File)
        fun buildBinaryAndRun(mainModuleKlibFile: File, dependencies: Dependencies)

        fun onNonEmptyBuildDirectory(directory: File)

        fun isIgnoredTest(projectInfo: ProjectInfo): Boolean = projectInfo.muted
        fun onIgnoredTest()
    }

    class Dependencies(val regularDependencies: Set<File>, val friendDependencies: Set<File>) {
        fun mergeWith(other: Dependencies): Dependencies =
            Dependencies(regularDependencies + other.regularDependencies, friendDependencies + other.friendDependencies)

        companion object {
            val EMPTY = Dependencies(emptySet(), emptySet())
        }
    }

    fun runTest(testConfiguration: TestConfiguration) = with(testConfiguration) {
        val projectName = testDir.name

        val projectInfoFile = File(testDir, PROJECT_INFO_FILE)
        val projectInfo: ProjectInfo = ProjectInfoParser(projectInfoFile).parse(projectName)

        if (isIgnoredTest(projectInfo)) {
            return onIgnoredTest() // Ignore muted tests.
        }

        val modulesMap: Map<String, ModuleUnderTest> = buildMap {
            projectInfo.modules.forEach { moduleName ->
                val moduleTestDir = File(testDir, moduleName)
                KtUsefulTestCase.assertExists(moduleTestDir)

                val moduleInfoFile = File(moduleTestDir, MODULE_INFO_FILE)
                val moduleInfo = ModuleInfoParser(moduleInfoFile).parse(moduleName)

                val moduleBuildDirs = createModuleDirs(buildDir, moduleName)

                // Populate the source dir with *.kt files.
                copySources(from = moduleTestDir, to = moduleBuildDirs.sourceDir)

                // Include ABI utils into the main module.
                if (moduleName == MAIN_MODULE_NAME) {
                    val utilsDir = testDir.parentFile.resolve(ABI_UTILS_DIR)
                    KtUsefulTestCase.assertExists(utilsDir)

                    copySources(from = utilsDir, to = moduleBuildDirs.sourceDir) { contents ->
                        contents.replace(TEST_MODE_PLACEHOLDER, testModeName)
                    }
                }

                val moduleOutputDir = moduleBuildDirs.outputDir.apply { mkdirs() }
                val klibFile = moduleOutputDir.resolve("$moduleName.klib")

                this[moduleName] = ModuleUnderTest(
                    info = moduleInfo,
                    testDir = moduleTestDir,
                    buildDirs = moduleBuildDirs,
                    klibFile = klibFile
                )
            }
        }

        // Collect all dependencies for building the final binary file.
        var binaryDependencies = Dependencies.EMPTY

        projectInfo.steps.forEach { projectStep ->
            projectStep.order.forEach { moduleName ->
                val (moduleInfo, moduleTestDir, moduleBuildDirs, klibFile) = modulesMap[moduleName]
                    ?: fail { "No module $moduleName found on step ${projectStep.id}" }

                val moduleStep = moduleInfo.steps[projectStep.id]

                moduleStep.modifications.forEach { modification ->
                    modification.execute(moduleTestDir, moduleBuildDirs.sourceDir)
                }

                if (!moduleBuildDirs.outputDir.list().isNullOrEmpty())
                    onNonEmptyBuildDirectory(moduleBuildDirs.outputDir)

                val regularDependencies = hashSetOf<File>()
                val friendDependencies = hashSetOf<File>()

                moduleStep.dependencies.forEach { dependency ->
                    if (dependency.moduleName == "stdlib")
                        regularDependencies += stdlibFile
                    else {
                        val moduleFile = modulesMap[dependency.moduleName]?.klibFile
                            ?: fail { "No module ${dependency.moduleName} found on step ${projectStep.id}" }
                        regularDependencies += moduleFile
                        if (dependency.isFriend) friendDependencies += moduleFile
                    }
                }

                val dependencies = Dependencies(regularDependencies, friendDependencies)
                binaryDependencies = binaryDependencies.mergeWith(dependencies)

                buildKlib(moduleInfo.moduleName, moduleBuildDirs.sourceDir, dependencies, klibFile)
            }
        }

        val mainModuleKlibFile = modulesMap[MAIN_MODULE_NAME]?.klibFile ?: fail { "No main module $MAIN_MODULE_NAME found" }
        binaryDependencies = binaryDependencies.mergeWith(Dependencies(setOf(mainModuleKlibFile), emptySet()))

        buildBinaryAndRun(mainModuleKlibFile, binaryDependencies)
    }

    private fun copySources(from: File, to: File, patchSourceFile: ((String) -> String)? = null) {
        var anyFilePatched = false

        from.walk().filter { it.isFile && it.extension == "kt" }.forEach { sourceFile ->
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

    fun createModuleDirs(buildDir: File, moduleName: String): ModuleBuildDirs {
        val moduleBuildDir = buildDir.resolve(moduleName)

        val moduleSourceDir = moduleBuildDir.resolve(SOURCE_DIR_NAME).apply { mkdirs() }
        val moduleOutputDir = moduleBuildDir.resolve(OUTPUT_DIR_NAME).apply { mkdirs() }

        return ModuleBuildDirs(moduleSourceDir, moduleOutputDir)
    }

    data class ModuleBuildDirs(val sourceDir: File, val outputDir: File) {
        internal companion object {
            const val SOURCE_DIR_NAME = "sources"
            const val OUTPUT_DIR_NAME = "outputs"
        }
    }

    private data class ModuleUnderTest(
        val info: ModuleInfo,
        val testDir: File,
        val buildDirs: ModuleBuildDirs,
        val klibFile: File
    )

    const val MAIN_MODULE_NAME = "main"
    private const val ABI_UTILS_DIR = "__utils__"
    private const val TEST_MODE_PLACEHOLDER = "TestMode.__UNKNOWN__"
}
