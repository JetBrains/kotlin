/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.ModuleBuildDirs.Companion.OUTPUT_DIR_NAME
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.ModuleBuildDirs.Companion.SOURCE_DIR_NAME
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.fail
import java.io.File

object PartialLinkageTestUtils {
    interface TestConfiguration {
        val testDir: File
        val buildDir: File
        val stdlibFile: File
        val testModeConstructorParameters: Map<String, String>

        // Customize the source code of a module before compiling it to a KLIB.
        fun customizeModuleSources(moduleName: String, moduleSourceDir: File)

        // Build a KLIB from a module.
        fun buildKlib(
            moduleName: String,
            buildDirs: ModuleBuildDirs,
            dependencies: Dependencies,
            klibFile: File,
            compilerEdition: KlibCompilerEdition,
        )

        // Build a binary (executable) file given the main KLIB and the rest of dependencies.
        fun buildBinaryAndRun(mainModule: Dependency, otherDependencies: Dependencies)

        // Take measures if the build directory is non-empty before the compilation
        // (ex: backup the previously generated artifacts stored in the build directory).
        fun onNonEmptyBuildDirectory(directory: File)

        // A way to check if a test is ignored or not. Override this function if necessary.
        fun isIgnoredTest(projectInfo: ProjectInfo): Boolean = projectInfo.muted

        // How to handle the test that is known to be ignored.
        fun onIgnoredTest()
    }

    data class Dependency(val moduleName: String, val libraryFile: File)

    class Dependencies(val regularDependencies: Set<Dependency>, val friendDependencies: Set<Dependency>) {
        init {
            regularDependencies.checkNoDuplicates("regular")
            regularDependencies.checkNoDuplicates("friend")
        }

        fun mergeWith(other: Dependencies): Dependencies =
            Dependencies(regularDependencies + other.regularDependencies, friendDependencies + other.friendDependencies)

        companion object {
            val EMPTY = Dependencies(emptySet(), emptySet())

            private fun Set<Dependency>.checkNoDuplicates(kind: String) {
                fun Map<String, List<Dependency>>.dump(): String = values.flatten().sortedBy { it.moduleName }.joinToString()

                val duplicatedModules = groupBy { it.moduleName }.filterValues { it.size > 1 }
                assertTrue(duplicatedModules.isEmpty()) {
                    "There are duplicated $kind module dependencies: ${duplicatedModules.dump()}"
                }

                val duplicatedFiles = groupBy { it.libraryFile.absolutePath }.filterValues { it.size > 1 }
                assertTrue(duplicatedFiles.isEmpty()) {
                    "There are $kind module dependencies with conflicting paths: ${duplicatedFiles.dump()}"
                }
            }
        }
    }

    fun runTest(
        testConfiguration: TestConfiguration,
        compilerEditionChange: KlibCompilerChangeScenario = KlibCompilerChangeScenario.NoChange,
    ) =
        with(testConfiguration) {
            val projectName = testDir.name

            val projectInfoFile = File(testDir, PROJECT_INFO_FILE)
            val projectInfo: ProjectInfo = ProjectInfoParser(projectInfoFile, "").parse(projectName)

            if (isIgnoredTest(projectInfo)) {
                return onIgnoredTest() // Ignore muted tests.
            }

            val modulesMap: Map<String, ModuleUnderTest> = buildMap {
                projectInfo.modules.forEach { moduleName ->
                    val moduleTestDir = File(testDir, moduleName)
                    KtUsefulTestCase.assertExists(moduleTestDir)

                    val moduleInfoFile = File(moduleTestDir, MODULE_INFO_FILE)
                    val moduleInfo = ModuleInfoParser(moduleInfoFile, "").parse(moduleName)

                    val moduleBuildDirs = createModuleDirs(buildDir, moduleName)

                    // Populate the source dir with *.kt files.
                    copySources(from = moduleTestDir, to = moduleBuildDirs.sourceDir)

                    // Customize the source dir if necessary.
                    customizeModuleSources(moduleName, moduleBuildDirs.sourceDir)

                    // Include PL utils into the main module.
                    if (moduleName == MAIN_MODULE_NAME) {
                        val utilsDir = testDir.parentFile.parentFile.resolve(PL_UTILS_DIR)
                        KtUsefulTestCase.assertExists(utilsDir)

                        copySources(from = utilsDir, to = moduleBuildDirs.sourceDir) { contents ->
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

                    moduleBuildDirs.outputDir.apply { mkdirs() }

                    this[moduleName] = ModuleUnderTest(
                        info = moduleInfo,
                        testDir = moduleTestDir,
                        buildDirs = moduleBuildDirs
                    )
                }
            }

            // Collect all dependencies for building the final binary file.
            var binaryDependencies = Dependencies.EMPTY

            projectInfo.steps.forEach { projectStep ->
                projectStep.order.forEach { moduleName ->
                    val moduleUnderTest = modulesMap[moduleName] ?: fail { "No module $moduleName found on step ${projectStep.id}" }
                    val (moduleInfo, moduleTestDir, moduleBuildDirs) = moduleUnderTest
                    val moduleStep = moduleInfo.steps.getValue(projectStep.id)

                    moduleStep.modifications.forEach { modification ->
                        modification.execute(moduleTestDir, moduleBuildDirs.sourceDir)
                    }

                    if (!moduleBuildDirs.outputDir.list().isNullOrEmpty())
                        onNonEmptyBuildDirectory(moduleBuildDirs.outputDir)

                    val regularDependencies = hashSetOf<Dependency>()
                    val friendDependencies = hashSetOf<Dependency>()

                    moduleStep.dependencies.forEach { dependency ->
                        if (dependency.moduleName == "stdlib")
                            regularDependencies += Dependency("stdlib", stdlibFile)
                        else {
                            val klibFile = modulesMap[dependency.moduleName]?.klibFile
                                ?: fail { "No module ${dependency.moduleName} found on step ${projectStep.id}" }
                            val moduleDependency = Dependency(dependency.moduleName, klibFile)
                            regularDependencies += moduleDependency
                            if (dependency.isFriend) friendDependencies += moduleDependency
                        }
                    }

                    val dependencies = Dependencies(regularDependencies, friendDependencies)
                    binaryDependencies = binaryDependencies.mergeWith(dependencies)

                    val compilerEdition = when (moduleStep.compiler) {
                        ModuleInfo.CompilerCase.BOTTOM_V1 -> compilerEditionChange.bottomV1
                        ModuleInfo.CompilerCase.BOTTOM_V2 -> compilerEditionChange.bottomV2
                        ModuleInfo.CompilerCase.INTERMEDIATE -> compilerEditionChange.intermediate
                        ModuleInfo.CompilerCase.DEFAULT -> KlibCompilerEdition.CURRENT
                    }

                    buildKlib(
                        moduleInfo.moduleName,
                        moduleBuildDirs,
                        dependencies,
                        moduleUnderTest.klibFile,
                        compilerEdition
                    )
                }
            }

            val mainModuleKlibFile = modulesMap[MAIN_MODULE_NAME]?.klibFile ?: fail { "No main module $MAIN_MODULE_NAME found" }
            val mainModuleDependency = Dependency(MAIN_MODULE_NAME, mainModuleKlibFile)

            buildBinaryAndRun(mainModuleDependency, binaryDependencies)
        }

    private fun copySources(from: File, to: File, patchSourceFile: ((String) -> String)? = null) {
        var anyFilePatched = false

        from.walk().filter { it.isFile && (it.extension == "kt" || it.extension == "js") }.forEach { sourceFile ->
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

        if (moduleBuildDir.exists()) moduleBuildDir.deleteRecursively()

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

    private data class ModuleUnderTest(val info: ModuleInfo, val testDir: File, val buildDirs: ModuleBuildDirs) {
        val klibFile get() = buildDirs.outputDir.resolve("${info.moduleName}.klib")
    }

    const val MAIN_MODULE_NAME = "main"
    private const val PL_UTILS_DIR = "__utils__"
    private const val TEST_MODE_PLACEHOLDER = "__UNKNOWN_TEST_MODE__"
}
