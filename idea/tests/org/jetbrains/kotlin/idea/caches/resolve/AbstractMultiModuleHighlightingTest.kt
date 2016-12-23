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

import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider
import org.jetbrains.kotlin.idea.stubs.AbstractMultiHighlightingTest
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.junit.Assert

abstract class AbstractMultiModuleHighlightingTest : AbstractMultiHighlightingTest() {

    override val testPath = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleHighlighting/"

    protected fun checkHighlightingInAllFiles() {
        var atLeastOneFile = false
        PluginJetFilesProvider.allFilesInProject(myProject!!).forEach { file ->
            if (!file.text.contains("// !CHECK_HIGHLIGHTING")) {
                atLeastOneFile = true
                configureByExistingFile(file.virtualFile!!)
                checkHighlighting(myEditor, true, false)
            }
        }
        Assert.assertTrue(atLeastOneFile)
    }
}
