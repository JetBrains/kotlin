/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.TestStructure.ModuleUnderTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.assertExists
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.fail
import java.io.File

/**
 * Utilities for building tests with repetitive KLIB-based compiler invocations.
 *
 * Examples:
 * - Partial linkage tests:
 *   - Build multiple KLIBs with dependencies between some of them
 *   - Rebuild and substitute some of those KLIBs
 *   - Finally, build a binary and run it
 * - KLIB compatibility tests:
 *   - Build KLIBs with one [KlibCompilerEdition]
 *   - Then build a binary with another [KlibCompilerEdition]
 */
object KlibCompilerInvocationTestUtils {
    interface TestConfiguration {
        val testDir: File
        val buildDir: File
        val stdlibFile: File
        val targetBackend: TargetBackend

        // A way to check if a test is ignored or not. Override this function if necessary.
        fun isIgnoredTest(projectInfo: ProjectInfo): Boolean {
            if (projectInfo.muted) return true
            val compatibleBackends = generateSequence(targetBackend) { if (it == TargetBackend.ANY) null else it.compatibleWith }.toSet()
            return projectInfo.ignoreBackends.intersect(compatibleBackends).isNotEmpty()
        }

        // How to handle the test that is known to be ignored.
        fun onIgnoredTest()
    }

    data class TestStructure(
        val projectInfo: ProjectInfo,
        val modules: Map<String, ModuleUnderTest>,
    ) {
        data class ModuleUnderTest(val moduleInfo: ModuleInfo, val testDataDir: File?, val sourceDir: File, val outputDir: File) {
            val klibFile get() = outputDir.resolve("${moduleInfo.moduleName}.klib")
        }
    }

    interface TestStructureExtractor {
        fun extractTestStructure(testDataPath: File): TestStructure
    }

    /** [BinaryArtifact] is a representation of a platform-specific binary artifact. */
    interface BinaryArtifact

    /** [ArtifactBuilder] component is responsible for building KLIB and binary artifacts in a platform-specific manner. */
    interface ArtifactBuilder<BA : BinaryArtifact> {
        /** Build a KLIB from a module. */
        fun buildKlib(
            module: ModuleUnderTest,
            dependencies: Dependencies,
            compilerEdition: KlibCompilerEdition,
            compilerArguments: List<String>,
        )

        /** Build a binary (executable) file given the main KLIB and the rest of dependencies. */
        fun buildBinary(mainModule: Dependency, otherDependencies: Dependencies): BA
    }

    /** [BinaryRunner] component is capable of running [BinaryArtifact]s in a platform-specific way. */
    interface BinaryRunner<BA : BinaryArtifact> {
        fun runBinary(binaryArtifact: BA)
    }

    data class Dependency(val moduleName: String, val libraryFile: File)

    class Dependencies(val regularDependencies: Set<Dependency>, val friendDependencies: Set<Dependency>) {
        init {
            regularDependencies.checkNoDuplicates("regular")
            friendDependencies.checkNoDuplicates("friend")
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

    fun <BA : BinaryArtifact> runTest(
        testConfiguration: TestConfiguration,
        testStructureExtractor: TestStructureExtractor,
        artifactBuilder: ArtifactBuilder<BA>,
        binaryRunner: BinaryRunner<BA>,
        compilerEditionChange: KlibCompilerChangeScenario,
    ) {
        val testStructure = testStructureExtractor.extractTestStructure(testDataPath = testConfiguration.testDir)

        if (testConfiguration.isIgnoredTest(testStructure.projectInfo)) {
            return testConfiguration.onIgnoredTest() // Ignore muted tests.
        }

        // Collect all dependencies for building the final binary file.
        var binaryDependencies = Dependencies.EMPTY

        testStructure.projectInfo.steps.forEach { projectStep ->
            projectStep.order.forEach { moduleName ->
                val moduleUnderTest = testStructure.modules[moduleName]
                    ?: fail { "No module $moduleName found on step ${projectStep.id}" }

                val (moduleInfo, moduleTestDataDir, moduleSourceDir, moduleOutputDir) = moduleUnderTest
                val moduleStep = moduleInfo.steps.getValue(projectStep.id)

                moduleStep.modifications.forEach { modification ->
                    moduleTestDataDir ?: fail { "No test data dir for module $moduleName on step ${projectStep.id}" }
                    modification.execute(moduleTestDataDir, moduleSourceDir)
                }

                moduleOutputDir.listFiles()?.forEach(File::deleteRecursively)

                val regularDependencies = hashSetOf<Dependency>()
                val friendDependencies = hashSetOf<Dependency>()

                moduleStep.dependencies.forEach { dependency ->
                    if (dependency.moduleName == "stdlib")
                        regularDependencies += Dependency("stdlib", testConfiguration.stdlibFile)
                    else {
                        val klibFile = testStructure.modules[dependency.moduleName]?.klibFile
                            ?: fail { "No module ${dependency.moduleName} found on step ${projectStep.id}" }
                        val moduleDependency = Dependency(dependency.moduleName, klibFile)
                        regularDependencies += moduleDependency
                        if (dependency.isFriend) friendDependencies += moduleDependency
                    }
                }

                val dependencies = Dependencies(regularDependencies, friendDependencies)
                binaryDependencies = binaryDependencies.mergeWith(dependencies)

                val compilerEdition = compilerEditionChange.getCompilerEditionForKlib(moduleStep.compilerCodename)

                artifactBuilder.buildKlib(
                    module = moduleUnderTest,
                    dependencies = dependencies,
                    compilerEdition = compilerEdition,
                    compilerArguments = moduleStep.cliArguments,
                )
            }
        }

        val mainModuleKlibFile = testStructure.modules[MAIN_MODULE_NAME]?.klibFile ?: fail { "No main module $MAIN_MODULE_NAME found" }
        val mainModuleDependency = Dependency(MAIN_MODULE_NAME, mainModuleKlibFile)

        val binaryArtifact = artifactBuilder.buildBinary(mainModuleDependency, binaryDependencies)
        binaryRunner.runBinary(binaryArtifact)
    }

    const val MAIN_MODULE_NAME = "main"
}

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

    companion object {
        const val MAIN_MODULE_NAME = "main"

        private const val SOURCE_DIR_NAME = "sources"
        private const val OUTPUT_DIR_NAME = "outputs"

        private const val PL_UTILS_DIR = "__utils__"
        private const val TEST_MODE_PLACEHOLDER = "__UNKNOWN_TEST_MODE__"
    }
}
