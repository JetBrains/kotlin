/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.application.PathMacros
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.testFramework.PlatformTestCase
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.configuration.KotlinWithLibraryConfigurator.FileState
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.IOException

abstract class AbstractConfigureKotlinTest : PlatformTestCase() {

    @Throws(Exception::class)
    override fun tearDown() {
        PathMacros.getInstance().removeMacro(TEMP_DIR_MACRO_KEY)

        super.tearDown()
    }

    @Throws(Exception::class)
    override fun initApplication() {
        super.initApplication()

        val tempLibDir = FileUtil.createTempDirectory("temp", null)
        PathMacros.getInstance().setMacro(TEMP_DIR_MACRO_KEY, FileUtilRt.toSystemDependentName(tempLibDir.absolutePath))
    }

    protected fun doTestConfigureModulesWithNonDefaultSetup(configurator: KotlinWithLibraryConfigurator) {
        assertNoFilesInDefaultPaths()

        val modules = modules
        for (module in modules) {
            assertNotConfigured(module, configurator)
        }

        configurator.configure(myProject, emptyList())

        assertNoFilesInDefaultPaths()

        for (module in modules) {
            assertProperlyConfigured(module, configurator)
        }
    }

    protected fun doTestOneJavaModule(jarState: FileState) {
        doTestOneModule(jarState, JAVA_CONFIGURATOR)
    }

    protected fun doTestOneJsModule(jarState: FileState) {
        doTestOneModule(jarState, JS_CONFIGURATOR)
    }

    private fun doTestOneModule(jarState: FileState, configurator: KotlinWithLibraryConfigurator) {
        val module = module

        assertNotConfigured(module, configurator)
        configure(module, jarState, configurator)
        assertProperlyConfigured(module, configurator)
    }

    override fun getModule(): Module {
        val modules = ModuleManager.getInstance(myProject).modules
        assert(modules.size == 1) { "One module should be loaded " + modules.size }
        myModule = modules[0]
        return super.getModule()
    }

    val modules: Array<Module>
        get() = ModuleManager.getInstance(myProject).modules

    @Throws(IOException::class)
    override fun getIprFile(): File {
        val projectFilePath = projectRoot + "/projectFile.ipr"
        TestCase.assertTrue("Project file should exists " + projectFilePath, File(projectFilePath).exists())
        return File(projectFilePath)
    }

    @Throws(Exception::class)
    override fun doCreateProject(projectFile: File): Project? {
        return myProjectManager.loadProject(projectFile.path)
    }

    private val projectName: String
        get() {
            val testName = getTestName(true)
            return if (testName.contains("_")) {
                testName.substring(0, testName.indexOf("_"))
            }
            else testName
        }

    protected val projectRoot: String
        get() = BASE_PATH + projectName

    override fun setUpModule() {}

    private fun assertNoFilesInDefaultPaths() {
        UsefulTestCase.assertDoesntExist(File(JAVA_CONFIGURATOR.getDefaultPathToJarFile(project)))
        UsefulTestCase.assertDoesntExist(File(JS_CONFIGURATOR.getDefaultPathToJarFile(project)))
    }

    companion object {
        private val BASE_PATH = "idea/testData/configuration/"
        private val TEMP_DIR_MACRO_KEY = "TEMP_TEST_DIR"
        protected val JAVA_CONFIGURATOR: KotlinJavaModuleConfigurator by lazy {
            object : KotlinJavaModuleConfigurator() {
                override fun getDefaultPathToJarFile(project: Project) = getPathRelativeToTemp("default_jvm_lib")
            }
        }

        protected val JS_CONFIGURATOR: KotlinJsModuleConfigurator by lazy {
            object : KotlinJsModuleConfigurator() {
                override fun getDefaultPathToJarFile(project: Project) = getPathRelativeToTemp("default_js_lib")
            }
        }

        private fun configure(
                modules: List<Module>,
                runtimeState: FileState,
                configurator: KotlinWithLibraryConfigurator,
                jarFromDist: String,
                jarFromTemp: String
        ) {
            val project = modules.iterator().next().project
            val collector = createConfigureKotlinNotificationCollector(project)

            val pathToJar = getPathToJar(runtimeState, jarFromDist, jarFromTemp)
            for (module in modules) {
                configurator.configureModuleWithLibrary(module, pathToJar, pathToJar, collector, runtimeState)
            }
            collector.showNotification()
        }

        private fun getPathToJar(runtimeState: FileState, jarFromDist: String, jarFromTemp: String): String {
            when (runtimeState) {
                KotlinWithLibraryConfigurator.FileState.EXISTS -> return jarFromDist
                KotlinWithLibraryConfigurator.FileState.COPY -> return jarFromTemp
                KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY -> return jarFromDist
            }
            return jarFromDist
        }

        protected fun configure(module: Module, jarState: FileState, configurator: KotlinProjectConfigurator) {
            if (configurator is KotlinJavaModuleConfigurator) {
                configure(listOf(module), jarState,
                          configurator as KotlinWithLibraryConfigurator,
                          pathToExistentRuntimeJar, pathToNonexistentRuntimeJar)
            }
            if (configurator is KotlinJsModuleConfigurator) {
                configure(listOf(module), jarState,
                          configurator as KotlinWithLibraryConfigurator,
                          pathToExistentJsJar, pathToNonexistentJsJar)
            }
        }

        private val pathToNonexistentRuntimeJar: String
            get() {
                val pathToTempKotlinRuntimeJar = FileUtil.getTempDirectory() + "/" + PathUtil.KOTLIN_JAVA_RUNTIME_JAR
                PlatformTestCase.myFilesToDelete.add(File(pathToTempKotlinRuntimeJar))
                return pathToTempKotlinRuntimeJar
            }

        private val pathToNonexistentJsJar: String
            get() {
                val pathToTempKotlinRuntimeJar = FileUtil.getTempDirectory() + "/" + PathUtil.JS_LIB_JAR_NAME
                PlatformTestCase.myFilesToDelete.add(File(pathToTempKotlinRuntimeJar))
                return pathToTempKotlinRuntimeJar
            }

        private val pathToExistentRuntimeJar: String
            get() = PathUtil.kotlinPathsForDistDirectory.stdlibPath.parent

        private val pathToExistentJsJar: String
            get() = PathUtil.kotlinPathsForDistDirectory.jsStdLibJarPath.parent

        protected fun assertNotConfigured(module: Module, configurator: KotlinWithLibraryConfigurator) {
            TestCase.assertFalse(
                    String.format("Module %s should not be configured as %s Module", module.name, configurator.presentableText),
                    configurator.isConfigured(module))
        }

        protected fun assertConfigured(module: Module, configurator: KotlinWithLibraryConfigurator) {
            TestCase.assertTrue(String.format("Module %s should be configured with configurator '%s'", module.name,
                                              configurator.presentableText),
                                configurator.isConfigured(module))
        }

        protected fun assertProperlyConfigured(module: Module, configurator: KotlinWithLibraryConfigurator) {
            assertConfigured(module, configurator)
            assertNotConfigured(module, getOppositeConfigurator(configurator))
        }

        private fun getOppositeConfigurator(configurator: KotlinWithLibraryConfigurator): KotlinWithLibraryConfigurator {
            if (configurator === JAVA_CONFIGURATOR) return JS_CONFIGURATOR
            if (configurator === JS_CONFIGURATOR) return JAVA_CONFIGURATOR

            throw IllegalArgumentException("Only JS_CONFIGURATOR and JAVA_CONFIGURATOR are supported")
        }

        private fun getPathRelativeToTemp(relativePath: String): String {
            val tempPath = PathMacros.getInstance().getValue(TEMP_DIR_MACRO_KEY)
            return tempPath + '/' + relativePath
        }
    }
}
