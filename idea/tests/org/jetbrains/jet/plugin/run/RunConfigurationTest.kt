/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.run

import com.intellij.testFramework.PlatformTestCase
import com.intellij.codeInsight.CodeInsightTestCase
import com.intellij.testFramework.PsiTestUtil
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.testFramework.MapDataContext
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.PsiLocation
import com.intellij.execution.Location
import org.jetbrains.jet.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.configurations.JavaParameters
import org.junit.Assert
import com.intellij.execution.configurations.JavaCommandLine
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import java.io.File
import com.intellij.openapi.roots.ModuleRootModificationUtil
import org.jetbrains.jet.plugin.search.allScope
import org.jetbrains.jet.plugin.refactoring.runWriteAction

class RunConfigurationTest: CodeInsightTestCase() {
    override fun getProject() = myProject!!
    override fun getModule() = myModule!!

    fun testMainInTest() {
        val createResult = configureModule(moduleDirPath("module"), getProject().getBaseDir()!!)

        val runConfiguration = createConfigurationFromMain("some.main")
        val javaParameters = getJavaRunParameters(runConfiguration)

        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(createResult.src))
        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(createResult.test))
    }

    fun testDependencyModuleClasspath() {
        val dependencyModuleSrcDir = configureModule(moduleDirPath("module"), getProject().getBaseDir()!!).src

        val moduleWithDependencyDir = runWriteAction { getProject().getBaseDir()!!.createChildDirectory(this, "moduleWithDependency") }!!

        val moduleWithDependency = createModule("moduleWithDependency")
        ModuleRootModificationUtil.setModuleSdk(moduleWithDependency, getTestProjectJdk())

        val moduleWithDependencySrcDir = configureModule(
                moduleDirPath("moduleWithDependency"), moduleWithDependencyDir, configModule = moduleWithDependency).src

        ModuleRootModificationUtil.addDependency(moduleWithDependency, getModule())

        val jetRunConfiguration = createConfigurationFromMain("some.test.main")
        jetRunConfiguration.setModule(moduleWithDependency)

        val javaParameters = getJavaRunParameters(jetRunConfiguration)

        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(dependencyModuleSrcDir))
        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(moduleWithDependencySrcDir))
    }

    private fun createConfigurationFromMain(mainFqn: String): JetRunConfiguration {
        val mainFunction = JetTopLevelFunctionsFqnNameIndex.getInstance()!!.get(mainFqn, getProject(), getProject().allScope())!!.first()

        val dataContext = MapDataContext()
        dataContext.put(Location.DATA_KEY, PsiLocation(getProject(), mainFunction))

        return ConfigurationContext.getFromContext(dataContext)!!.getConfiguration()!!.getConfiguration() as JetRunConfiguration
    }

    private fun configureModule(moduleDir: String, outputParentDir: VirtualFile, configModule: Module = getModule()): CreateModuleResult {
        val srcPath = moduleDir + "/src"
        PsiTestUtil.createTestProjectStructure(getProject(), configModule, srcPath, PlatformTestCase.myFilesToDelete, true)

        val testPath = moduleDir + "/test"
        if (File(testPath).exists()) {
            val testDir = PsiTestUtil.createTestProjectStructure(getProject(), configModule, testPath, PlatformTestCase.myFilesToDelete, false)
            PsiTestUtil.addSourceRoot(getModule(), testDir, true)
        }

        val (srcOutDir, testOutDir) = runWriteAction {
            val outDir = outputParentDir.createChildDirectory(this, "out")
            val srcOutDir = outDir.createChildDirectory(this, "production")
            val testOutDir = outDir.createChildDirectory(this, "test")

            PsiTestUtil.setCompilerOutputPath(configModule, srcOutDir.getUrl(), false)
            PsiTestUtil.setCompilerOutputPath(configModule, testOutDir.getUrl(), true)

            Pair(srcOutDir, testOutDir)
        }!!

        PsiDocumentManager.getInstance(getProject()).commitAllDocuments()

        return CreateModuleResult(configModule, srcOutDir, testOutDir)
    }

    private fun moduleDirPath(moduleName: String) = "${getTestDataPath()}${getTestName(false)}/$moduleName"

    private fun getJavaRunParameters(configuration: RunConfiguration): JavaParameters {
        val state = configuration.getState(MockExecutor, ExecutionEnvironment(MockProfile, MockExecutor, myProject!!, null))

        Assert.assertNotNull(state)
        Assert.assertTrue(state is JavaCommandLine)

        configuration.checkConfiguration()
        return (state as JavaCommandLine).getJavaParameters()!!
    }

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/run/"
    override fun getTestProjectJdk() = PluginTestCaseBase.jdkFromIdeaHome()

    private class CreateModuleResult(val module: Module, val src: VirtualFile, val test: VirtualFile)

    private object MockExecutor : DefaultRunExecutor() {
        override fun getId() = "mock"
    }

    private object MockProfile : RunProfile {
        override fun getState(executor: Executor, env: ExecutionEnvironment) = null
        override fun getIcon() = null
        override fun getName() = null
    }
}
