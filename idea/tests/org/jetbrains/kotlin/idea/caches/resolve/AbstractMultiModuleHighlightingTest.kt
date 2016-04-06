/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.ModuleRootModificationUtil
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider
import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.PsiTestUtil
import java.io.File
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.roots.DependencyScope
import com.intellij.testFramework.IdeaTestUtil
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.junit.Assert

abstract class AbstractMultiModuleHighlightingTest : DaemonAnalyzerTestCase() {

    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleHighlighting/"

    protected fun checkHighlightingInAllFiles() {
        var atLeastOneFile = false
        PluginJetFilesProvider.allFilesInProject(myProject!!).forEach { file ->
            atLeastOneFile = true
            configureByExistingFile(file.virtualFile!!)
            checkHighlighting(myEditor, true, false)
        }
        Assert.assertTrue(atLeastOneFile)
    }

    protected fun module(name: String, hasTestRoot: Boolean = false, useFullJdk: Boolean = false): Module {
        val srcDir = TEST_DATA_PATH + "${getTestName(true)}/$name"
        val moduleWithSrcRootSet = createModuleFromTestData(srcDir, "$name", StdModuleTypes.JAVA, true)!!
        if (hasTestRoot) {
            setTestRoot(moduleWithSrcRootSet, name)
        }

        val jdkToUse = if (useFullJdk) PluginTestCaseBase.fullJdk() else PluginTestCaseBase.mockJdk()
        ConfigLibraryUtil.configureSdk(moduleWithSrcRootSet, jdkToUse)

        return moduleWithSrcRootSet
    }

    protected fun setTestRoot(module: Module, name: String) {
        val testDir = TEST_DATA_PATH + "${getTestName(true)}/${name}Test"
        val testRootDirInTestData = File(testDir)
        val testRootDir = createTempDirectory()!!
        FileUtil.copyDir(testRootDirInTestData, testRootDir)
        val testRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(testRootDir)!!
        object : WriteCommandAction.Simple<Unit>(project) {
            override fun run() {
                testRoot.refresh(false, true)
            }
        }.execute().throwException()
        PsiTestUtil.addSourceRoot(module, testRoot, true)
    }

    protected fun Module.addDependency(
            other: Module,
            dependencyScope: DependencyScope = DependencyScope.COMPILE,
            exported: Boolean = false
    ) = ModuleRootModificationUtil.addDependency(this, other, dependencyScope, exported)
}
