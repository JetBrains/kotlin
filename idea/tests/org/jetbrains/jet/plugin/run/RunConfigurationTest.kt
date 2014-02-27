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
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import org.jetbrains.jet.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
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

class RunConfigurationTest: CodeInsightTestCase() {
    private val project: Project get() = myProject!!
    private val module: Module get() = myModule!!

    fun testMainInTest() {
        val outDirs = configureModule(getTestDataPath() + getTestName(false) + "/module")

        val runConfiguration = createRunConfiguration("some.main")
        val javaParameters = getJavaRunParameters(runConfiguration)

        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(outDirs.src))
        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(outDirs.test))
    }

    private fun createRunConfiguration(mainFqn: String): RunConfiguration {
        val mainFunction = JetTopLevelFunctionsFqnNameIndex.getInstance()!!.get(
                mainFqn, project, GlobalSearchScope.allScope(project))!!.first();

        val dataContext = MapDataContext()
        dataContext.put(CommonDataKeys.PROJECT, project)
        dataContext.put(LangDataKeys.MODULE, module)
        dataContext.put(Location.DATA_KEY, PsiLocation(project, mainFunction))

        return ConfigurationContext.getFromContext(dataContext)!!.getConfiguration()!!.getConfiguration()!!
    }

    private fun configureModule(moduleDir: String): ModuleOutputDirs {
        val srcPath = moduleDir + "/src"
        PsiTestUtil.createTestProjectStructure(project, module, srcPath, PlatformTestCase.myFilesToDelete, true)

        val testPath = moduleDir + "/test"
        val testDir = PsiTestUtil.createTestProjectStructure(project, module, testPath, PlatformTestCase.myFilesToDelete, false)
        PsiTestUtil.addSourceRoot(getModule(), testDir, true)

        val outDirs = ApplicationManager.getApplication()!!.runWriteAction(Computable<ModuleOutputDirs> {
            val outDir = project.getBaseDir()!!.createChildDirectory(this, "out")
            val srcOutDir = outDir.createChildDirectory(this, "production")
            val testOutDir = outDir.createChildDirectory(this, "test")

            PsiTestUtil.setCompilerOutputPath(module, srcOutDir.getUrl(), false)
            PsiTestUtil.setCompilerOutputPath(module, testOutDir.getUrl(), true)

            ModuleOutputDirs(srcOutDir, testOutDir)
        })!!

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        return outDirs
    }

    private fun getJavaRunParameters(configuration: RunConfiguration): JavaParameters {
        val state = configuration.getState(MOCK_EXECUTOR, ExecutionEnvironment(MockProfile(), MOCK_EXECUTOR, myProject!!, null))

        Assert.assertNotNull(state)
        Assert.assertTrue(state is JavaCommandLine)

        configuration.checkConfiguration()
        return ((state as JavaCommandLine)).getJavaParameters()!!
    }

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/run/"
    override fun getTestProjectJdk() = PluginTestCaseBase.jdkFromIdeaHome()

    private data class ModuleOutputDirs(val src: VirtualFile, val test: VirtualFile)

    private val MOCK_EXECUTOR: Executor = object : DefaultRunExecutor() {
        override fun getId(): String {
            return "mock"
        }
    }

    private class MockProfile() : RunProfile {
        override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? = null
        override fun getIcon() = null
        override fun getName() = null
    }
}
