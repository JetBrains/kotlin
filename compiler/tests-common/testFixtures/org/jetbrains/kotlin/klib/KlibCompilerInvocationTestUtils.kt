/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.TestStructure.ModuleUnderTest
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
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
        val buildDir: File
        val stdlibFile: File
        val targetBackend: TargetBackend

        // A way to check if a test is ignored or not. Override this function if necessary.
        fun isIgnoredTest(projectInfo: ProjectInfo): Boolean {
            if (projectInfo.muted) return true
            return !InTextDirectivesUtils.isCompatibleTarget(
                /* targetBackend = */ targetBackend,
                /* backends = */ projectInfo.targetBackends.toList(),
                /* doNotTarget = */ projectInfo.ignoreBackends.toList(),
            )
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
        fun buildBinary(
            mainModule: Dependency,
            otherDependencies: Dependencies,
            compilerEdition: KlibCompilerEdition,
        ): BA
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
        testStructure: TestStructure,
        testConfiguration: TestConfiguration,
        artifactBuilder: ArtifactBuilder<BA>,
        binaryRunner: BinaryRunner<BA>,
        compilerEditionChange: KlibCompilerChangeScenario,
    ) {
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

                artifactBuilder.buildKlib(
                    module = moduleUnderTest,
                    dependencies = dependencies,
                    compilerEdition = compilerEditionChange.getCompilerEditionForKlib(moduleStep.compilerCodename),
                    compilerArguments = moduleStep.cliArguments,
                )
            }
        }

        val mainModuleKlibFile = testStructure.modules[MAIN_MODULE_NAME]?.klibFile ?: fail { "No main module $MAIN_MODULE_NAME found" }
        val mainModuleDependency = Dependency(MAIN_MODULE_NAME, mainModuleKlibFile)

        val binaryArtifact = artifactBuilder.buildBinary(
            mainModule = mainModuleDependency,
            otherDependencies = binaryDependencies,
            compilerEdition = compilerEditionChange.getCompilerEditionForBinary(),
        )

        binaryRunner.runBinary(binaryArtifact)
    }

    const val MAIN_MODULE_NAME = "main"
}
