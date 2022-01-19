/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.klib.AbstractKlibABITestCase.Companion.ModuleBuildDirs.Companion.OUTPUT_DIR_NAME
import org.jetbrains.kotlin.klib.AbstractKlibABITestCase.Companion.ModuleBuildDirs.Companion.SOURCE_DIR_NAME
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.jupiter.api.fail
import java.io.File
import kotlin.io.path.createTempDirectory

abstract class AbstractKlibABITestCase : KtUsefulTestCase() {
    protected lateinit var buildDir: File
    protected lateinit var environment: KotlinCoreEnvironment

    override fun setUp() {
        super.setUp()
        buildDir = createTempDirectory().toFile().also { it.mkdirs() }

        environment = KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            CompilerConfiguration(),
            EnvironmentConfigFiles.JS_CONFIG_FILES
        )
    }

    override fun tearDown() {
        buildDir.deleteRecursively()
    }

    protected abstract fun stdlibFile(): File

    protected abstract fun buildKlib(
        moduleName: String,
        moduleSourceDir: File,
        moduleDependencies: Collection<File>,
        klibFile: File
    )

    protected abstract fun buildBinaryAndRun(
        mainModuleKlibFile: File,
        libraries: Collection<File>
    )

    fun doTest(testPath: String) = Companion.doTest(
        testDir = File(testPath).absoluteFile,
        buildDir = buildDir,
        stdlibFile = stdlibFile(),
        buildKlib = ::buildKlib,
        buildBinaryAndRun = ::buildBinaryAndRun
    )

    companion object {
        fun doTest(
            testDir: File,
            buildDir: File,
            stdlibFile: File,
            buildKlib: (moduleName: String, moduleSourceDir: File, moduleDependencies: Collection<File>, klibFile: File) -> Unit,
            buildBinaryAndRun: (mainModuleKlibFile: File, allDependencies: Collection<File>) -> Unit
        ) {
            val projectName = testDir.name

            val projectInfoFile = File(testDir, PROJECT_INFO_FILE)
            val projectInfo: ProjectInfo = ProjectInfoParser(projectInfoFile).parse(projectName)

            val modulesMap: Map<String, ModuleUnderTest> = buildMap {
                projectInfo.modules.forEach { moduleName ->
                    val moduleTestDir = File(testDir, moduleName)
                    assertExists(moduleTestDir)

                    val moduleInfoFile = File(moduleTestDir, MODULE_INFO_FILE)
                    val moduleInfo = ModuleInfoParser(moduleInfoFile).parse(moduleName)

                    val moduleBuildDirs = createModuleDirs(buildDir, moduleName)

                    // Populate the source dir with *.kt files.
                    val moduleSourceDir = moduleBuildDirs.sourceDir.apply { mkdirs() }
                    moduleTestDir.walk().filter { it.isFile && it.extension == "kt" }.forEach { sourceFile ->
                        val destFile = moduleSourceDir.resolve(sourceFile.relativeTo(moduleTestDir))
                        destFile.parentFile.mkdirs()
                        sourceFile.copyTo(destFile)
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

            projectInfo.steps.forEach { projectStep ->
                projectStep.order.forEach { moduleName ->
                    val (moduleInfo, moduleTestDir, moduleBuildDirs, klibFile) = modulesMap[moduleName]
                        ?: fail { "No module $moduleName found on step ${projectStep.id}" }

                    val moduleStep = moduleInfo.steps[projectStep.id]

                    moduleStep.modifications.forEach { modification ->
                        modification.execute(moduleTestDir, moduleBuildDirs.sourceDir)
                    }

                    if (klibFile.exists()) klibFile.delete() // TODO: maybe backup?

                    val moduleDependencies = moduleStep.dependencies.map { dependencyName ->
                        if (dependencyName == "stdlib")
                            stdlibFile
                        else
                            modulesMap[dependencyName]?.klibFile ?: fail { "No module $dependencyName found on step ${projectStep.id}" }
                    }

                    buildKlib(moduleInfo.moduleName, moduleBuildDirs.sourceDir, moduleDependencies, klibFile)
                }
            }

            val mainModuleKlibFile = modulesMap[MAIN_MODULE_NAME]?.klibFile ?: fail { "No main module $MAIN_MODULE_NAME found" }
            val allKlibs = buildSet {
                this += stdlibFile
                modulesMap.mapTo(this) { (_, module) -> module.klibFile }
            }

            buildBinaryAndRun(mainModuleKlibFile, allKlibs)
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
    }
}