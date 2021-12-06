/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
import kotlin.io.path.createTempDirectory

abstract class AbstractKlibABITestCase : KtUsefulTestCase() {

    private fun parseProjectInfo(testName: String, infoFile: File): ProjectInfo {
        return ProjectInfoParser(infoFile).parse(testName)
    }

    private fun parseModuleInfo(moduleName: String, infoFile: File): ModuleInfo {
        return ModuleInfoParser(infoFile).parse(moduleName)
    }

    abstract fun compileBinaryAndRun(project: Project, configuration: CompilerConfiguration, libraries: Collection<String>, mainModulePath: String, buildDir: File)

    abstract fun stdlibPath(): String

    fun doTest(testPath: String) {

        val testDir = File(testPath)
        val testName = testDir.name
        val projectInfoFile = File(testDir, PROJECT_INFO_FILE)

        val projectInfo = parseProjectInfo(testName, projectInfoFile)

        val modulesMap = mutableMapOf<String, ModuleInfo>()

        for (module in projectInfo.modules) {
            val moduleDir = File(testDir, module)
            assert(moduleDir.exists())
            val moduleInfoFile = File(moduleDir, MODULE_INFO_FILE)
            modulesMap[module] = parseModuleInfo(module, moduleInfoFile)
        }

        val disposable = TestDisposable()
        val environment = createEnvironment(disposable)
        val buildDir = createTempDirectory().toFile().also { it.mkdirs() }

        try {
            doTestImpl(environment, testDir, buildDir, projectInfo, modulesMap)
        } finally {
            buildDir.deleteRecursively()
            disposable.dispose()
        }
    }


    private fun makeDirectoriesForModule(moduleName: String, buildDir: File): File {
        val moduleDir = File(buildDir, moduleName)
        File(moduleDir, SOURCE_DIR_NAME).mkdirs()
        File(moduleDir, KLIB_DIR_NAME).mkdirs()
        moduleDir.deleteOnExit()
        return moduleDir
    }

    private fun prepareBuildDirs(testDir: File, buildDir: File, projectInfo: ProjectInfo): Map<String, File> {
        val modulesBuildDirs = projectInfo.modules.associateWith { makeDirectoriesForModule(it, buildDir) }

        for (module in projectInfo.modules) {
            val moduleTestDir = File(testDir, module).also { assert(it.exists()) }
            val moduleBuildDir = modulesBuildDirs[module] ?: error("No dir found for module $module")
            val moduleSourceDir = File(moduleBuildDir, SOURCE_DIR_NAME)
            moduleTestDir.listFiles { _, name -> name.endsWith(".kt") }!!.forEach {
                val sourceFile = File(moduleSourceDir, it.name)
                it.copyTo(sourceFile)
            }
        }

        return modulesBuildDirs
    }

    private fun createEnvironment(disposable: TestDisposable): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(disposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    }

    private fun collectDependenciesKlib(buildDir: File, dependencies: Collection<String>): List<String> {
        val result = ArrayList<String>(dependencies.size)


        return dependencies.mapTo(result) { dep ->
            if (dep == "stdlib") {
                stdlibPath()
            } else {
                val depBuildDir = File(buildDir, dep)
                depBuildDir.toKlibFile(dep).canonicalPath
            }
        }
    }

    private fun File.toKlibFile(name: String): File {
        return File(File(this, KLIB_DIR_NAME), "$name.klib")
    }

    private fun doTestImpl(
        environment: KotlinCoreEnvironment,
        testDir: File,
        buildDir: File,
        projectInfo: ProjectInfo,
        modulesInfo: Map<String, ModuleInfo>
    ) {
        val modulesBuildDirs = prepareBuildDirs(testDir, buildDir, projectInfo)

        for (projectStep in projectInfo.steps) {
            for (module in projectStep.order) {
                val moduleTestDir = File(testDir, module)
                val moduleBuildDir = modulesBuildDirs[module] ?: error("No module dir found for $module")
                val moduleInfo = modulesInfo[module] ?: error("No module $module found on step ${projectStep.id}")
                val moduleStep = moduleInfo.steps[projectStep.id]
                val moduleSourceDir = File(moduleBuildDir, SOURCE_DIR_NAME)
                for (modification in moduleStep.modifications) {
                    modification.execute(moduleTestDir, moduleSourceDir)
                }

                val klibFile = moduleBuildDir.toKlibFile(module)
                if (klibFile.exists()) klibFile.delete()
                val dependencies = collectDependenciesKlib(buildDir, moduleStep.dependencies)
                buildKlib(environment, module, moduleSourceDir, dependencies, klibFile)
            }
        }

        val mainModuleDir = modulesBuildDirs[MAIN_MODULE_NAME] ?: error("No main module $MAIN_MODULE_NAME found")

        val moduleKlibs = collectAllModulesKlibs(modulesBuildDirs)

        compileBinaryAndRun(environment.project, environment.configuration, moduleKlibs, mainModuleDir.toKlibFile(MAIN_MODULE_NAME).canonicalPath, buildDir)
    }

    private fun collectAllModulesKlibs(modulesBuildDirs: Map<String, File>): Collection<String> {
        val result = ArrayList<String>(modulesBuildDirs.size + 1)
        result.add(stdlibPath())

        for ((name, dir) in modulesBuildDirs) {
            result.add(dir.toKlibFile(name).canonicalPath)
        }

        return result
    }


    private fun KotlinCoreEnvironment.createPsiFiles(sourceDir: File): Collection<KtFile> {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) as CoreLocalFileSystem

        val sourceFile = sourceDir.listFiles()!!.first()
        val vFile = fileSystem.findFileByIoFile(sourceFile) ?: error("Virtual File for $sourceFile not found")

        val provider = SingleRootFileViewProvider(psiManager, vFile)
        val allfiles = provider.allFiles
        return allfiles.mapNotNull { it as? KtFile }
    }

    abstract fun buildKlibImpl(
        project: Project,
        configuration: CompilerConfiguration,
        moduleName: String,
        sources: Collection<KtFile>,
        dependencies: Collection<String>,
        outputFile: File
    )

    private fun buildKlib(
        environment: KotlinCoreEnvironment,
        moduleName: String,
        sourceDir: File,
        dependencies: List<String>,
        outputFile: File
    ) {
        val ktFiles = environment.createPsiFiles(sourceDir)
        val config = environment.configuration.copy()
        config.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        buildKlibImpl(environment.project, config, moduleName, ktFiles, dependencies, outputFile)
    }

    companion object {
        private const val SOURCE_DIR_NAME = "sources"
        private const val KLIB_DIR_NAME = "klibs"

        const val MAIN_MODULE_NAME = "main"
    }


}