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

package org.jetbrains.jet.plugin.caches.resolve

import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.ModuleRootModificationUtil
import org.jetbrains.jet.plugin.project.PluginJetFilesProvider
import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.openapi.module.Module

class MultiModuleHighlightingTest : DaemonAnalyzerTestCase() {

    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleHighlighting/"

    fun testVisibility() {
        val module1 = module("m1")
        val module2 = module("m2")

        module2.addDependency(module1)

        checkHighlightingInAllFiles()
    }

    fun testDependency() {
        val module1 = module("m1")
        val module2 = module("m2")
        val module3 = module("m3")
        val module4 = module("m4")

        module2.addDependency(module1)

        module1.addDependency(module2)

        module3.addDependency(module2)

        module4.addDependency(module1)
        module4.addDependency(module2)
        module4.addDependency(module3)

        checkHighlightingInAllFiles()
    }

    private fun checkHighlightingInAllFiles() {
        PluginJetFilesProvider.allFilesInProject(myProject!!).forEach { file ->
            configureByExistingFile(file.getVirtualFile()!!)
            checkHighlighting(myEditor, true, false)
        }
    }

    private fun module(name: String): Module {
        return createModuleFromTestData(TEST_DATA_PATH + "${getTestName(true)}/$name", "$name", StdModuleTypes.JAVA, true)!!
    }

    private fun Module.addDependency(other: Module) = ModuleRootModificationUtil.addDependency(this, other)
}