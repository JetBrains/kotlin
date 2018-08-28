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

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.jetbrains.kotlin.idea.stubs.AbstractMultiHighlightingTest
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.allJavaFiles
import org.jetbrains.kotlin.idea.test.allKotlinFiles
import java.io.File

abstract class AbstractMultiModuleHighlightingTest : AbstractMultiHighlightingTest() {

    protected open fun checkHighlightingInProject(
        findFiles: () -> List<PsiFile> = { project.allKotlinFiles().excludeByDirective() }
    ) {
        checkFiles(findFiles) {
            checkHighlighting(myEditor, true, false)
        }
    }
}

abstract class AbstractMultiPlatformHighlightingTest : AbstractMultiModuleHighlightingTest() {

    protected open fun doTest(path: String) {
        setupMppProjectFromDirStructure(File(path))
        checkHighlightingInProject {
            (project.allKotlinFiles() + project.allJavaFiles()).excludeByDirective()
        }
    }

    override fun getTestDataPath() = "${PluginTestCaseBase.getTestDataPathBase()}/multiModuleHighlighting/multiplatform/"
}

private fun List<PsiFile>.excludeByDirective() = filter { !it.text.contains("// !CHECK_HIGHLIGHTING") }